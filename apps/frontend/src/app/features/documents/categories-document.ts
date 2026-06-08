/**
 * Catégories documentaires reconnues par le backend (document-service).
 * <p>
 * Liste de référence partagée entre le dialog de dépôt (sélecteur de catégorie) et la
 * page Documents (filtre par catégorie). Les valeurs correspondent aux codes « base »
 * de l'énuméré {@code document_category} et au @Pattern des DTO côté serveur.
 * <p>
 * Conformément à l'énoncé, le courrier est décliné en arrivé / départ et les notes de
 * service en interne / externe ; les catégories génériques historiques (« courrier »,
 * « note de service ») sont conservées pour rester compatibles avec l'existant.
 */
export interface OptionCategorie {
  valeur: string;
  libelle: string;
}

/** Catégories proposées au dépôt et au filtrage. */
export const CATEGORIES_DOCUMENT: OptionCategorie[] = [
  { valeur: 'courrier_arrive', libelle: 'Courrier arrivé' },
  { valeur: 'courrier_depart', libelle: 'Courrier départ' },
  { valeur: 'note_service_interne', libelle: 'Note de service interne' },
  { valeur: 'note_service_externe', libelle: 'Note de service externe' },
  { valeur: 'note_administrative', libelle: 'Note administrative' },
  { valeur: 'circulaire', libelle: 'Circulaire' },
  { valeur: 'rapport', libelle: 'Rapport' },
  { valeur: 'compte_rendu', libelle: 'Compte rendu' },
  { valeur: 'autre', libelle: 'Autre' },
];
