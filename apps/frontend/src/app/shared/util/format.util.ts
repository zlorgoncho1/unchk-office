// ============================================================
// Petites fonctions utilitaires d'affichage (formatage FR), partagées
// par les dashboards. Aucune dépendance Angular : fonctions pures.
// ============================================================

// Formate un nombre en séparant les milliers (espace insécable fine, style FR).
export function formaterNombre(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) {
    return '0';
  }
  return new Intl.NumberFormat('fr-FR').format(n);
}

// Formate un montant monétaire (devise par défaut : XOF / franc CFA).
export function formaterMontant(
  montant: number | null | undefined,
  devise = 'XOF'
): string {
  if (montant == null || Number.isNaN(montant)) {
    return '—';
  }
  try {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: devise,
      maximumFractionDigits: 0,
    }).format(montant);
  } catch {
    // Devise inconnue : repli sur un nombre suffixé.
    return `${formaterNombre(montant)} ${devise}`;
  }
}

// Formate une date ISO (jj/mm/aaaa) ; renvoie « — » si absente/invalide.
export function formaterDate(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return '—';
  }
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(d);
}

// Formate une date-heure ISO (jj/mm/aaaa à HH:MM).
export function formaterDateHeure(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return '—';
  }
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(d);
}

// Calcule un pourcentage entier borné [0, 100] (réalisé / prévu, etc.).
export function pourcentage(
  partie: number | null | undefined,
  total: number | null | undefined
): number {
  if (!total || total <= 0 || partie == null) {
    return 0;
  }
  return Math.min(100, Math.round((partie / total) * 100));
}

// Remplace les underscores par des espaces et met la première lettre en capitale.
export function humaniser(valeur: string | null | undefined): string {
  if (!valeur) {
    return '—';
  }
  const txt = valeur.replace(/_/g, ' ');
  return txt.charAt(0).toUpperCase() + txt.slice(1);
}
