import { bootstrapApplication } from '@angular/platform-browser';
import 'iconify-icon'; // Enregistre le web component <iconify-icon> (jeu d'icônes Solar)
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// Point d'entrée de l'application Angular (composant racine autonome).
bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
