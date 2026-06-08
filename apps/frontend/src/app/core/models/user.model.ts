import { Role } from './role.model';

// Utilisateur authentifié, reconstruit à partir des claims du JWT.
export interface User {
  // UUID de l'utilisateur (claim sub).
  id: string;
  // Courriel (login).
  email: string;
  // Nom complet affiché.
  fullName: string;
  // Rôles accordés.
  roles: Role[];
}

// Réponse d'authentification du gateway (POST /api/identity/auth/login | /refresh).
// Champs alignés sur le record backend ReponseJetons.
export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // toujours "Bearer"
  expiresIn: number; // durée de vie de l'access token (secondes)
  userId: string;
  roles: Role[];
}

// Corps de la requête de connexion.
// ATTENTION : le backend attend le champ "motDePasse" (et non "password").
export interface LoginRequest {
  email: string;
  motDePasse: string;
}

// Corps des requêtes de rafraîchissement / déconnexion.
export interface RefreshRequest {
  refreshToken: string;
}
