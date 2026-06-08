import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';

import { environment } from '../../../environments/environment';
import { AppNotification } from '../models';
import { AuthService } from '../auth/auth.service';

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

  private socket: WebSocket | null = null;
  private timerReconnexion: ReturnType<typeof setTimeout> | null = null;
  private fermetureVolontaire = false;

  // Notifications reçues (les plus récentes en tête).
  private readonly _notifications = signal<AppNotification[]>([]);
  readonly notifications = this._notifications.asReadonly();

  // État de la connexion temps réel.
  private readonly _etat = signal<EtatRealtime>('deconnecte');
  readonly etat = this._etat.asReadonly();

  // Nombre de notifications non lues (badge).
  readonly nonLues = computed(
    () => this._notifications().filter((n) => !n.lu).length
  );

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

  /** Marque une notification comme lue (mise à jour optimiste locale). */
  marquerLue(id: string): void {
    this._notifications.update((liste) =>
      liste.map((n) => (n.id === id ? { ...n, lu: true } : n))
    );
  }

  /** Vide la liste locale des notifications. */
  vider(): void {
    this._notifications.set([]);
  }

  // --- Interne ---

  // Décode un message reçu et l'ajoute à la liste s'il est exploitable.
  private traiterMessage(evt: MessageEvent): void {
    try {
      const data = JSON.parse(evt.data as string) as AppNotification;
      if (data && typeof data.id === 'string') {
        this._notifications.update((liste) => [data, ...liste]);
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
