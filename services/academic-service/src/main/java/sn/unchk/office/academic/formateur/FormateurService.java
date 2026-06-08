package sn.unchk.office.academic.formateur;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.academic.formateur.dto.AffectationCreationDto;
import sn.unchk.office.academic.formation.FormationService;
import sn.unchk.office.common.audit.AuditLogger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Logique d'affectation des formateurs et de lecture de la projection locale.
 * <p>
 * Le nom d'un formateur est toujours résolu via le read-model {@code academic_formateur_ro}
 * (alimenté par {@code people.staff}), jamais par un appel REST vers people-service.
 */
@Service
public class FormateurService {

    private final AffectationFormateurRepository affectationRepository;
    private final FormateurRoRepository formateurRoRepository;
    private final FormationService formationService;
    private final AuditLogger auditLogger;

    public FormateurService(AffectationFormateurRepository affectationRepository,
                            FormateurRoRepository formateurRoRepository,
                            FormationService formationService,
                            AuditLogger auditLogger) {
        this.affectationRepository = affectationRepository;
        this.formateurRoRepository = formateurRoRepository;
        this.formationService = formationService;
        this.auditLogger = auditLogger;
    }

    /** Liste tous les formateurs connus localement (projection). */
    @Transactional(readOnly = true)
    public List<FormateurRo> listerFormateurs() {
        return formateurRoRepository.findAll();
    }

    /** Liste les affectations d'une formation (vérifie d'abord que la formation existe). */
    @Transactional(readOnly = true)
    public List<AffectationFormateur> listerAffectations(UUID formationId) {
        // Lève 404 si la formation n'existe pas / est supprimée.
        formationService.obtenir(formationId);
        return affectationRepository.findByIdFormationId(formationId);
    }

    /**
     * Affecte un formateur à une formation pour un module donné.
     *
     * @param formationId formation cible (doit exister)
     * @param dto         données d'affectation validées
     */
    @Transactional
    public AffectationFormateur affecter(UUID formationId, AffectationCreationDto dto) {
        // Vérifie l'existence de la formation (404 sinon).
        formationService.obtenir(formationId);

        AffectationFormateur affectation =
                new AffectationFormateur(formationId, dto.formateurRef(), dto.module());
        AffectationFormateur enregistree = affectationRepository.save(affectation);
        auditLogger.succes("AFFECTATION_FORMATEUR", "formation", formationId.toString());
        return enregistree;
    }

    /**
     * Retire l'affectation d'un formateur (formation + formateur + module).
     */
    @Transactional
    public void retirerAffectation(UUID formationId, UUID formateurRef, String module) {
        formationService.obtenir(formationId);
        AffectationFormateurId id = new AffectationFormateurId(formationId, formateurRef, module);
        affectationRepository.deleteById(id);
        auditLogger.succes("RETRAIT_FORMATEUR", "formation", formationId.toString());
    }

    /**
     * Résout en une seule fois les noms d'une série de formateurs depuis la projection locale.
     *
     * @param refs identifiants de formateurs (people.staff.id)
     * @return table id -> nom complet (les inconnus sont absents de la table)
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> resoudreNoms(List<UUID> refs) {
        return formateurRoRepository.findAllById(refs).stream()
                .collect(Collectors.toMap(FormateurRo::getId, FormateurRo::getFullName));
    }

    /** Résout le nom d'un seul formateur depuis la projection ({@code null} si inconnu). */
    @Transactional(readOnly = true)
    public String resoudreNom(UUID ref) {
        if (ref == null) {
            return null;
        }
        return formateurRoRepository.findById(ref).map(FormateurRo::getFullName).orElse(null);
    }

    /** Fonction utilitaire de résolution de nom (utilisable pour mapper des listes). */
    public Function<UUID, String> resolveurNom(Map<UUID, String> noms) {
        return ref -> ref != null ? noms.get(ref) : null;
    }
}
