// Les 5 rôles applicatifs de la plateforme UNCHK Office.
// Source de vérité : authz.rego + docs/specifications.md.
export type Role =
  | 'admin'
  | 'administratif'
  | 'enseignant'
  | 'appui-insertion'
  | 'etudiant';

// Liste exhaustive des rôles (pour itérer / valider).
export const ROLES: readonly Role[] = [
  'admin',
  'administratif',
  'enseignant',
  'appui-insertion',
  'etudiant',
] as const;

// Libellés lisibles (français) pour l'affichage dans l'interface.
export const LIBELLES_ROLES: Record<Role, string> = {
  admin: 'Administrateur',
  administratif: 'Personnel administratif',
  enseignant: 'Enseignant',
  'appui-insertion': "Appui à l'insertion",
  etudiant: 'Étudiant',
};

// Indique si une chaîne quelconque correspond à un rôle connu.
export function estRole(valeur: string): valeur is Role {
  return (ROLES as readonly string[]).includes(valeur);
}
