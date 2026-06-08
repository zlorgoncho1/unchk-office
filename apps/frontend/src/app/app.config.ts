import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { routes } from './app.routes';
import { authInterceptor } from './core/http/auth.interceptor';

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
  ],
};
