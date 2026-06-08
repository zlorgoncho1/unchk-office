package sn.unchk.office.insertion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.insertion.domain.InsertionKind;
import sn.unchk.office.insertion.dto.StatistiquesInsertion;
import sn.unchk.office.insertion.projection.AcademicFormationRo;
import sn.unchk.office.insertion.repository.AcademicFormationRoRepository;
import sn.unchk.office.insertion.repository.InsertionOutcomeRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calcul des statistiques d'insertion (auto-emploi vs emploi salarié), globales et par formation.
 * <p>
 * S'appuie uniquement sur les données locales : la table {@code insertion_outcomes} pour les
 * effectifs et le read-model {@code academic_formation_ro} pour les libellés de formation.
 * AUCUN appel REST inter-service.
 */
@Service
public class StatistiquesService {

    private final InsertionOutcomeRepository outcomes;
    private final AcademicFormationRoRepository formations;

    public StatistiquesService(InsertionOutcomeRepository outcomes,
                               AcademicFormationRoRepository formations) {
        this.outcomes = outcomes;
        this.formations = formations;
    }

    /**
     * Construit les statistiques d'insertion à partir des situations courantes.
     */
    @Transactional(readOnly = true)
    public StatistiquesInsertion calculer() {
        // Répartition globale par type d'insertion.
        Map<String, Long> parType = nouvelleRepartition();
        long total = 0;
        for (Object[] ligne : outcomes.compterParTypeCourant()) {
            InsertionKind kind = (InsertionKind) ligne[0];
            long effectif = ((Number) ligne[1]).longValue();
            parType.merge(kind.name(), effectif, Long::sum);
            total += effectif;
        }

        // Répartition par formation (libellé via le read-model).
        Map<UUID, StatFormationAccumulateur> parFormation = new LinkedHashMap<>();
        for (Object[] ligne : outcomes.compterParFormationEtTypeCourant()) {
            UUID formationRef = (UUID) ligne[0];
            InsertionKind kind = (InsertionKind) ligne[1];
            long effectif = ((Number) ligne[2]).longValue();
            parFormation
                    .computeIfAbsent(formationRef, this::nouvelAccumulateur)
                    .ajouter(kind, effectif);
        }

        List<StatistiquesInsertion.StatistiqueFormation> detail = new ArrayList<>();
        for (StatFormationAccumulateur acc : parFormation.values()) {
            detail.add(acc.versDto());
        }

        return new StatistiquesInsertion(total, parType, detail);
    }

    private StatFormationAccumulateur nouvelAccumulateur(UUID formationRef) {
        String libelle = formationRef != null
                ? formations.findById(formationRef).map(AcademicFormationRo::getLabel).orElse("n/a")
                : "n/a";
        return new StatFormationAccumulateur(formationRef, libelle);
    }

    /** Initialise une répartition avec tous les types à zéro (lecture stable côté client). */
    private Map<String, Long> nouvelleRepartition() {
        Map<String, Long> repartition = new LinkedHashMap<>();
        for (InsertionKind kind : InsertionKind.values()) {
            repartition.put(kind.name(), 0L);
        }
        return repartition;
    }

    /** Accumulateur interne par formation. */
    private final class StatFormationAccumulateur {
        private final UUID formationRef;
        private final String libelle;
        private final Map<String, Long> parType = nouvelleRepartition();
        private long total;

        StatFormationAccumulateur(UUID formationRef, String libelle) {
            this.formationRef = formationRef;
            this.libelle = libelle;
        }

        void ajouter(InsertionKind kind, long effectif) {
            parType.merge(kind.name(), effectif, Long::sum);
            total += effectif;
        }

        StatistiquesInsertion.StatistiqueFormation versDto() {
            return new StatistiquesInsertion.StatistiqueFormation(
                    formationRef != null ? formationRef.toString() : null,
                    libelle, total, parType);
        }
    }
}
