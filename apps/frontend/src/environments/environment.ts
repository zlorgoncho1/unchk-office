// Environnement de PRODUCTION (configuration par défaut).
// Le frontend appelle uniquement l'API Gateway (seul point d'entrée REST + WebSocket).
export const environment = {
  production: true,
  // Base de l'API REST (gateway). Les chemins métier sont préfixés par /api/...
  apiBaseUrl: 'http://localhost:8080',
  // Base WebSocket pour les notifications temps réel (/ws/notifications).
  wsUrl: 'ws://localhost:8080',
};
