// Notification temps réel poussée par communication-service (via WebSocket).
// Forme indicative ; le contrat exact sera affiné à l'intégration du module Communication.
export interface AppNotification {
  // Identifiant de la notification (UUID).
  id: string;
  // Type d'événement (ex. "compte-rendu.publie", "reunion.planifiee").
  type: string;
  // Libellé lisible affiché à l'utilisateur.
  libelle: string;
  // Ressource liée (URL ou UUID), optionnelle.
  ressource?: string;
  // Statut de lecture.
  lu: boolean;
  // Horodatage ISO 8601.
  horodatage: string;
}
