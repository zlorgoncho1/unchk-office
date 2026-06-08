import { JwtPayload } from '../models/jwt.model';
import { Role, estRole } from '../models/role.model';

// Décode (sans vérifier la signature : la validation RS256 est faite au gateway)
// la charge utile d'un JWT. Retourne null si le jeton est malformé.
export function decoderJwt(token: string): JwtPayload | null {
  const parties = token.split('.');
  if (parties.length !== 3) {
    return null;
  }
  try {
    const charge = parties[1];
    const json = decoderBase64Url(charge);
    const brut = JSON.parse(json) as Record<string, unknown>;

    // On ne conserve que des rôles connus (filtrage défensif).
    const rolesBruts = Array.isArray(brut['roles']) ? (brut['roles'] as unknown[]) : [];
    const roles = rolesBruts
      .filter((r): r is string => typeof r === 'string')
      .filter(estRole) as Role[];

    return {
      sub: typeof brut['sub'] === 'string' ? (brut['sub'] as string) : '',
      roles,
      email: typeof brut['email'] === 'string' ? (brut['email'] as string) : '',
      name: typeof brut['name'] === 'string' ? (brut['name'] as string) : '',
      iss: typeof brut['iss'] === 'string' ? (brut['iss'] as string) : undefined,
      aud: brut['aud'] as string | string[] | undefined,
      iat: typeof brut['iat'] === 'number' ? (brut['iat'] as number) : undefined,
      exp: typeof brut['exp'] === 'number' ? (brut['exp'] as number) : undefined,
    };
  } catch {
    return null;
  }
}

// Indique si le jeton est expiré (avec une marge de sécurité en secondes).
export function jwtExpire(payload: JwtPayload | null, margeSecondes = 0): boolean {
  if (!payload?.exp) {
    return true;
  }
  const maintenant = Math.floor(Date.now() / 1000);
  return payload.exp <= maintenant + margeSecondes;
}

// Décodage base64url -> chaîne UTF-8 (gère les caractères accentués).
function decoderBase64Url(valeur: string): string {
  // Conversion base64url -> base64 standard, puis ajout du remplissage.
  let base64 = valeur.replace(/-/g, '+').replace(/_/g, '/');
  const reste = base64.length % 4;
  if (reste) {
    base64 += '='.repeat(4 - reste);
  }
  const binaire = atob(base64);
  // Reconstruction des octets pour un décodage UTF-8 correct.
  const octets = Uint8Array.from(binaire, (c) => c.charCodeAt(0));
  return new TextDecoder().decode(octets);
}
