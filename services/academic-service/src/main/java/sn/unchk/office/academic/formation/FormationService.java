package sn.unchk.office.academic.formation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.academic.formation.dto.FormationCreationDto;
import sn.unchk.office.academic.formation.dto.FormationMajDto;
import sn.unchk.office.academic.formation.event.FormationEventPublisher;
import sn.unchk.office.common.audit.AuditLogger;

import java.util.List;
import java.util.UUID;

/**
 * Logique métier des formations : création, mise à jour, suppression logique et lecture.
 * <p>
 * Chaque modification persiste l'état local PUIS publie l'événement correspondant sur
 * {@code academic.formations} (afin d'alimenter les projections des autres services).
 * Les règles de cohérence (période, unicité de code) sont vérifiées ici et traduites en 400/409
 * par le gestionnaire d'erreurs commun.
 */
@Service
public class FormationService {

    private final FormationRepository formationRepository;
    private final FormationEventPublisher eventPublisher;
    private final AuditLogger auditLogger;

    public FormationService(FormationRepository formationRepository,
                            FormationEventPublisher eventPublisher,
                            AuditLogger auditLogger) {
        this.formationRepository = formationRepository;
        this.eventPublisher = eventPublisher;
        this.auditLogger = auditLogger;
    }

    /** Liste les formations non supprimées. */
    @Transactional(readOnly = true)
    public List<Formation> lister() {
        return formationRepository.findByDeletedAtIsNull();
    }

    /** Liste les formations non supprimées d'un niveau donné. */
    @Transactional(readOnly = true)
    public List<Formation> listerParNiveau(NiveauFormation niveau) {
        return formationRepository.findByLevelAndDeletedAtIsNull(niveau);
    }

    /**
     * Récupère une formation par son identifiant (non supprimée).
     *
     * @throws FormationIntrouvableException si elle n'existe pas / est supprimée
     */
    @Transactional(readOnly = true)
    public Formation obtenir(UUID id) {
        return formationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new FormationIntrouvableException(id));
    }

    /**
     * Crée une formation, la persiste et publie l'événement {@code Created}.
     *
     * @param dto      données validées
     * @param createur identifiant de l'utilisateur créateur (claim sub)
     */
    @Transactional
    public Formation creer(FormationCreationDto dto, UUID createur) {
        if (dto.code() != null && !dto.code().isBlank() && formationRepository.existsByCode(dto.code())) {
            // Conflit d'unicité de code -> 409 via le gestionnaire d'erreurs.
            throw new CodeFormationDejaUtiliseException(dto.code());
        }
        verifierPeriode(dto.startDate(), dto.endDate());

        Formation formation = new Formation();
        formation.setCode(dto.code());
        formation.setLabel(dto.label());
        formation.setLevel(dto.level());
        if (dto.kind() != null) {
            formation.setKind(dto.kind());
        }
        formation.setFunding(dto.funding());
        formation.setAmount(dto.amount());
        formation.setStartDate(dto.startDate());
        formation.setEndDate(dto.endDate());
        formation.setTrainedMale(dto.trainedMale() != null ? dto.trainedMale() : 0);
        formation.setTrainedFemale(dto.trainedFemale() != null ? dto.trainedFemale() : 0);
        formation.setResponsibleRef(dto.responsibleRef());
        formation.setActive(true);
        formation.setCreatedBy(createur);

        Formation enregistree = formationRepository.save(formation);
        eventPublisher.publierCreation(enregistree);
        auditLogger.succes("CREATION_FORMATION", "formation", enregistree.getId().toString());
        return enregistree;
    }

    /**
     * Met à jour une formation existante, la persiste et publie l'événement {@code Updated}.
     */
    @Transactional
    public Formation mettreAJour(UUID id, FormationMajDto dto) {
        Formation formation = obtenir(id);

        if (dto.code() != null && !dto.code().equals(formation.getCode())
                && formationRepository.existsByCode(dto.code())) {
            throw new CodeFormationDejaUtiliseException(dto.code());
        }
        verifierPeriode(dto.startDate(), dto.endDate());

        formation.setCode(dto.code());
        formation.setLabel(dto.label());
        formation.setLevel(dto.level());
        if (dto.kind() != null) {
            formation.setKind(dto.kind());
        }
        formation.setFunding(dto.funding());
        formation.setAmount(dto.amount());
        formation.setStartDate(dto.startDate());
        formation.setEndDate(dto.endDate());
        if (dto.trainedMale() != null) {
            formation.setTrainedMale(dto.trainedMale());
        }
        if (dto.trainedFemale() != null) {
            formation.setTrainedFemale(dto.trainedFemale());
        }
        formation.setResponsibleRef(dto.responsibleRef());
        if (dto.active() != null) {
            formation.setActive(dto.active());
        }

        Formation enregistree = formationRepository.save(formation);
        eventPublisher.publierMiseAJour(enregistree);
        auditLogger.succes("MAJ_FORMATION", "formation", enregistree.getId().toString());
        return enregistree;
    }

    /**
     * Supprime logiquement une formation et publie l'événement {@code Deleted} (tombstone logique).
     *
     * @param id      identifiant de la formation
     * @param auteur  identifiant de l'utilisateur qui supprime
     */
    @Transactional
    public void supprimer(UUID id, UUID auteur) {
        Formation formation = obtenir(id);
        formation.supprimerLogiquement();
        formationRepository.save(formation);
        eventPublisher.publierSuppression(id, auteur);
        auditLogger.succes("SUPPRESSION_FORMATION", "formation", id.toString());
    }

    /** Vérifie la cohérence de la période (fin >= début si les deux sont renseignées). */
    private void verifierPeriode(java.time.LocalDate debut, java.time.LocalDate fin) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin doit être postérieure ou égale à la date de début.");
        }
    }
}
