import {
  CUSTOM_ELEMENTS_SCHEMA,
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../../core/auth/auth.service';
import { LoginRequest } from '../../../core/models';

/**
 * Page de connexion brandée UNCHK.
 * Carte centrée sur fond clair, logo vertical, champs Material, gestion d'erreur.
 * Au succès, redirige vers l'URL de retour (ou /accueil par défaut).
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  // Requis pour le web component Iconify <iconify-icon>.
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Formulaire de connexion (email + mot de passe).
  readonly formulaire = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required, Validators.minLength(8)]],
  });

  // Année courante (pied de page).
  readonly annee = new Date().getFullYear();

  // États d'interface réactifs.
  readonly chargement = signal(false);
  readonly messageErreur = signal<string | null>(null);
  readonly motDePasseVisible = signal(false);

  /** Bascule l'affichage du mot de passe en clair. */
  basculerVisibilite(): void {
    this.motDePasseVisible.update((v) => !v);
  }

  /** Soumet les identifiants au gateway. */
  soumettre(): void {
    if (this.formulaire.invalid || this.chargement()) {
      this.formulaire.markAllAsTouched();
      return;
    }

    this.chargement.set(true);
    this.messageErreur.set(null);

    const identifiants = this.formulaire.getRawValue() as LoginRequest;

    this.auth.login(identifiants).subscribe({
      next: () => {
        this.chargement.set(false);
        // Redirige vers l'URL demandée avant la connexion, sinon /accueil.
        const retour =
          this.route.snapshot.queryParamMap.get('returnUrl') ?? '/accueil';
        void this.router.navigateByUrl(retour);
      },
      error: (erreur: unknown) => {
        this.chargement.set(false);
        this.messageErreur.set(this.messageDepuisErreur(erreur));
      },
    });
  }

  // Traduit une erreur HTTP en message lisible (sans fuite d'information).
  private messageDepuisErreur(erreur: unknown): string {
    if (erreur instanceof HttpErrorResponse) {
      if (erreur.status === 401 || erreur.status === 400) {
        return 'Identifiants incorrects. Veuillez réessayer.';
      }
      if (erreur.status === 0) {
        return 'Service indisponible. Vérifiez votre connexion.';
      }
      if (erreur.status === 429) {
        return 'Trop de tentatives. Réessayez dans un instant.';
      }
    }
    return 'Une erreur est survenue. Veuillez réessayer.';
  }
}
