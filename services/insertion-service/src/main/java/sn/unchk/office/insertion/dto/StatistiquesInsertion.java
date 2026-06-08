package sn.unchk.office.insertion.dto;

import java.util.List;
import java.util.Map;

/**
 * Statistiques d'insertion globales (auto-emploi vs emploi salarié).
 *
 * @param total          nombre total de situations courantes prises en compte
 * @param parType        répartition par situation d'insertion (clé = code, valeur = effectif)
 * @param parFormation   répartition détaillée par formation
 */
public record StatistiquesInsertion(
        long total,
        Map<String, Long> parType,
        List<StatistiqueFormation> parFormation
) {

    /**
     * Détail par formation : libellé (issu du read-model academic_formation_ro) et
     * répartition des situations d'insertion.
     *
     * @param formationRef    identifiant de la formation (peut être null si non renseignée)
     * @param formationLabel  libellé de la formation (« n/a » si non projetée)
     * @param total           effectif total pour cette formation
     * @param parType         répartition par situation d'insertion
     */
    public record StatistiqueFormation(
            String formationRef,
            String formationLabel,
            long total,
            Map<String, Long> parType
    ) {
    }
}
