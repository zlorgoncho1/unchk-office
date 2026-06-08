// Environnement de DÉVELOPPEMENT (ng serve sur http://localhost:4200).
// Même configuration que la prod en local : le gateway tourne sur le port 8080.
export const environment = {
  production: false,
  // Base de l'API REST (gateway). Les chemins métier sont préfixés par /api/...
  apiBaseUrl: 'http://localhost:8080',
  // Base WebSocket pour les notifications temps réel (/ws/notifications).
  wsUrl: 'ws://localhost:8080',
};
