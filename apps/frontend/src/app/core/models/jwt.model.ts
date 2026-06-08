import { Role } from './role.model';

// Charge utile (payload) du JWT RS256 émis par identity-service.
// Claims posés côté backend : sub, roles, email, name, iss, aud, iat, exp.
export interface JwtPayload {
  // Sujet : UUID de l'utilisateur.
  sub: string;
  // Rôles accordés (claim "roles").
  roles: Role[];
  // Courriel de l'utilisateur.
  email: string;
  // Nom complet.
  name: string;
  // Émetteur déclaré (iss).
  iss?: string;
  // Audience déclarée (aud).
  aud?: string | string[];
  // Émission (iat) et expiration (exp), en secondes Unix.
  iat?: number;
  exp?: number;
}
