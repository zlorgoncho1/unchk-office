import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { routes } from './app.routes';
import { authInterceptor } from './core/http/auth.interceptor';

// Locale FR enregistrée pour les pipes date/nombre/devise d'Angular.
registerLocaleData(localeFr);

// Configuration globale de l'application (providers autonomes).
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(), // animations Angular Material
    // Client HTTP + intercepteur d'authentification (Bearer + refresh sur 401).
    provideHttpClient(withInterceptors([authInterceptor])),
    // Graphes : enregistre tous les contrôleurs/échelles Chart.js par défaut.
    provideCharts(withDefaultRegisterables()),
    // Français par défaut (formats date/nombre cohérents avec l'interface).
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    // Champs Material homogènes PARTOUT : contour + libellé toujours flottant
    // (supprime l'incohérence label-flottant vs label-placeholder entre création/édition).
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: { appearance: 'outline', floatLabel: 'always' },
    },
  ],
};
