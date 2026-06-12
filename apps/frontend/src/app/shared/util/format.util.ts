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

// Formate un montant en version COMPACTE pour les cartes KPI (ex. « 6,33 Md FCFA »,
// « 12,5 M FCFA ») : évite les très longs nombres qui débordent des tuiles.
export function formaterMontantCompact(
  montant: number | null | undefined,
  devise = 'F CFA'
): string {
  if (montant == null || Number.isNaN(montant)) {
    return '—';
  }
  // Le code ISO XOF s'affiche « F CFA » (cohérent avec le reste de l'interface) ;
  // espace insécable pour que « F CFA » ne se coupe jamais en fin de ligne.
  const dev = devise === 'XOF' || devise === 'F CFA' ? 'F\u00A0CFA' : devise;
  const abs = Math.abs(montant);
  const fmt = (n: number, d: number): string =>
    new Intl.NumberFormat('fr-FR', { maximumFractionDigits: d }).format(n);
  if (abs >= 1_000_000_000) {
    return `${fmt(montant / 1_000_000_000, 2)} Md ${dev}`;
  }
  if (abs >= 1_000_000) {
    return `${fmt(montant / 1_000_000, 1)} M ${dev}`;
  }
  if (abs >= 1_000) {
    return `${fmt(montant / 1_000, 0)} k ${dev}`;
  }
  return `${fmt(montant, 0)} ${dev}`;
}

// Formate une taille de fichier en octets vers une unité lisible (o / Ko / Mo / Go).
// Évite d'afficher des octets bruts (« 1 234 567 ») dans les tableaux de documents.
export function formaterTaille(octets: number | null | undefined): string {
  if (octets == null || Number.isNaN(octets) || octets < 0) {
    return '—';
  }
  if (octets < 1024) {
    return `${octets} o`;
  }
  const unites = ['Ko', 'Mo', 'Go', 'To'];
  let taille = octets / 1024;
  let i = 0;
  while (taille >= 1024 && i < unites.length - 1) {
    taille /= 1024;
    i++;
  }
  const arrondi = taille >= 100 ? Math.round(taille) : Math.round(taille * 10) / 10;
  return `${new Intl.NumberFormat('fr-FR').format(arrondi)} ${unites[i]}`;
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
// Libellés français ACCENTUÉS pour les valeurs d'énumération courantes (statuts, types,
// situations…) : évite les « Termine / Prevu / Emploi salarie » sans accents.
const LABELS_HUMANISES: Record<string, string> = {
  prevu: 'Prévu', en_cours: 'En cours', termine: 'Terminé', terminee: 'Terminée',
  valide: 'Validé', rompu: 'Rompu', planifiee: 'Planifiée', annulee: 'Annulée',
  brouillon: 'Brouillon', publie: 'Publié', archive: 'Archivé',
  inscrit: 'Inscrit', diplome: 'Diplômé', suspendu: 'Suspendu', abandon: 'Abandon',
  cloture: 'Clôturé', vote: 'Voté', en_execution: 'En exécution',
  emploi_salarie: 'Emploi salarié', auto_emploi: 'Auto-emploi',
  recherche_emploi: 'En recherche d’emploi', poursuite_etudes: 'Poursuite d’études',
  sans_activite: 'Sans activité',
  conseil_universite: 'Conseil d’Université', seminaire: 'Séminaire', reunion: 'Réunion',
  evaluation: 'Évaluation', preparation_cours: 'Préparation de cours', tutorat: 'Tutorat',
};

export function humaniser(valeur: string | null | undefined): string {
  if (!valeur) {
    return '—';
  }
  const accentue = LABELS_HUMANISES[valeur.toLowerCase()];
  if (accentue) {
    return accentue;
  }
  const txt = valeur.replace(/_/g, ' ');
  return txt.charAt(0).toUpperCase() + txt.slice(1);
}
