import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';

import { environment } from '../../../environments/environment';
import { AppNotification } from '../models';
import { AuthService } from '../auth/auth.service';
import { CommunicationService, NotificationApi } from '../data';

// Chemin du canal de notifications temps réel (proxifié par le gateway).
const CHEMIN_WS = '/ws/notifications';
// Délai de reconnexion (ms) après une coupure non volontaire.
const DELAI_RECONNEXION_MS = 5000;

// État de la connexion temps réel.
export type EtatRealtime = 'deconnecte' | 'connexion' | 'connecte';

/**
 * Service de notifications temps réel.
 * <p>
 * Établit une connexion WebSocket vers {@code /ws/notifications} (via le gateway) et
 * expose la liste des notifications reçues sous forme de signal, ainsi que l'état de
 * connexion et le nombre de non-lues.
 * <p>
 * NOTE : le jeton est transmis en paramètre d'URL ({@code ?access_token=...}) car les
 * navigateurs n'autorisent pas l'en-tête Authorization sur l'ouverture d'un WebSocket.
 * Le backend communication-service utilise STOMP : le branchement applicatif complet
 * (souscription à {@code /user/queue/notifications}) sera finalisé à l'étape Communication ;
 * ce service fournit le socle de connexion et l'état réactif.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly api = inject(CommunicationService);

  private socket: WebSocket | null = null;
  private timerReconnexion: ReturnType<typeof setTimeout> | null = null;
  private fermetureVolontaire = false;

  // Notifications reçues (les plus récentes en tête).
  private readonly _notifications = signal<AppNotification[]>([]);
  readonly notifications = this._notifications.asReadonly();

  // État de la connexion temps réel.
  private readonly _etat = signal<EtatRealtime>('deconnecte');
  readonly etat = this._etat.asReadonly();

  // Nombre de notifications non lues (badge), dérivé de la liste affichée.
  readonly nonLues = computed(
    () => this._notifications().filter((n) => !n.lu).length
  );

  // Compteur de non-lues renvoyé par le backend (source de vérité serveur).
  private readonly _nonLuesServeur = signal(0);
  readonly nonLuesServeur = this._nonLuesServeur.asReadonly();

  constructor() {
    // Nettoyage automatique à la destruction du service (fin de vie de l'app).
    this.destroyRef.onDestroy(() => this.deconnecter());
  }

  /** Ouvre la connexion WebSocket si l'utilisateur est authentifié. */
  connecter(): void {
    if (this.socket || !this.auth.isAuthenticated()) {
      return;
    }
    this.fermetureVolontaire = false;
    this._etat.set('connexion');

    const token = this.auth.getAccessToken() ?? '';
    const url = `${environment.wsUrl}${CHEMIN_WS}?access_token=${encodeURIComponent(token)}`;

    try {
      this.socket = new WebSocket(url);
    } catch {
      this._etat.set('deconnecte');
      this.planifierReconnexion();
      return;
    }

    this.socket.onopen = () => this._etat.set('connecte');
    this.socket.onmessage = (evt) => this.traiterMessage(evt);
    this.socket.onclose = () => this.gererFermeture();
    this.socket.onerror = () => this.socket?.close();
  }

  /** Ferme la connexion volontairement (déconnexion utilisateur). */
  deconnecter(): void {
    this.fermetureVolontaire = true;
    this.annulerReconnexion();
    if (this.socket) {
      this.socket.onclose = null; // évite la reconnexion automatique
      this.socket.close();
      this.socket = null;
    }
    this._etat.set('deconnecte');
  }

  /**
   * Charge l'historique des notifications depuis le backend (REST) en complément du
   * flux WebSocket, et rafraîchit le compteur de non-lues. À appeler à l'ouverture de
   * la page : on fusionne l'historique avec ce qui a déjà pu arriver par socket (dédup par id).
   */
  chargerHistorique(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    this.api.listerNotifications().subscribe({
      next: (liste) => this.fusionnerHistorique(liste.map((n) => versNotification(n))),
      // En cas d'échec (endpoint indisponible), on garde simplement le flux temps réel.
      error: () => undefined,
    });
    this.rafraichirCompteur();
  }

  /**
   * Marque une notification comme lue : appelle le PATCH backend puis met à jour la
   * liste locale. Mise à jour optimiste immédiate pour une UI réactive, confirmée par
   * le serveur (et le compteur de non-lues rafraîchi).
   */
  marquerLue(id: string): void {
    // Mise à jour optimiste locale.
    this._notifications.update((liste) =>
      liste.map((n) => (n.id === id ? { ...n, lu: true } : n))
    );
    this.api.marquerNotificationLue(id).subscribe({
      next: () => this.rafraichirCompteur(),
      // Échec serveur : on resynchronise depuis l'historique pour ne pas mentir à l'UI.
      error: () => this.chargerHistorique(),
    });
  }

  /** Vide la liste locale des notifications. */
  vider(): void {
    this._notifications.set([]);
  }

  // --- Interne ---

  // Rafraîchit le compteur de non-lues depuis le backend (badge cloche).
  private rafraichirCompteur(): void {
    this.api.compterNotificationsNonLues().subscribe({
      next: (r) => this._nonLuesServeur.set(r?.count ?? 0),
      error: () => undefined,
    });
  }

  // Fusionne l'historique REST avec la liste courante (dédup par id, plus récent d'abord).
  private fusionnerHistorique(historique: AppNotification[]): void {
    this._notifications.update((courant) => {
      const parId = new Map<string, AppNotification>();
      // L'historique d'abord, puis le temps réel écrase (état lu/non-lu le plus frais).
      for (const n of historique) {
        parId.set(n.id, n);
      }
      for (const n of courant) {
        parId.set(n.id, n);
      }
      return [...parId.values()].sort(
        (a, b) => Date.parse(b.horodatage) - Date.parse(a.horodatage)
      );
    });
  }

  // Décode un message reçu (NotificationDto backend) et l'ajoute à la liste s'il est exploitable.
  private traiterMessage(evt: MessageEvent): void {
    try {
      const data = JSON.parse(evt.data as string) as NotificationApi;
      if (data && typeof data.id === 'string') {
        const notif = versNotification(data);
        // Dédup : si elle existe déjà (déjà chargée par l'historique), on la remplace en tête.
        this._notifications.update((liste) => [
          notif,
          ...liste.filter((n) => n.id !== notif.id),
        ]);
        this.rafraichirCompteur();
      }
    } catch {
      // Message non-JSON (ex. trame de contrôle) : ignoré.
    }
  }

  // Réagit à une fermeture de socket : reconnexion si non volontaire.
  private gererFermeture(): void {
    this.socket = null;
    this._etat.set('deconnecte');
    if (!this.fermetureVolontaire) {
      this.planifierReconnexion();
    }
  }

  // Programme une tentative de reconnexion différée.
  private planifierReconnexion(): void {
    if (this.timerReconnexion || this.fermetureVolontaire) {
      return;
    }
    this.timerReconnexion = setTimeout(() => {
      this.timerReconnexion = null;
      this.connecter();
    }, DELAI_RECONNEXION_MS);
  }

  // Annule une reconnexion programmée.
  private annulerReconnexion(): void {
    if (this.timerReconnexion) {
      clearTimeout(this.timerReconnexion);
      this.timerReconnexion = null;
    }
  }
}

// Convertit une notification backend (NotificationApi) vers le modèle d'affichage AppNotification.
function versNotification(n: NotificationApi): AppNotification {
  // Le libellé affiché reprend le titre, complété par le message si disponible.
  const libelle = n.message ? `${n.title} — ${n.message}` : n.title;
  return {
    id: n.id,
    type: n.kind,
    libelle,
    ressource: n.targetRef ?? undefined,
    lu: n.read,
    horodatage: n.createdAt,
  };
}
