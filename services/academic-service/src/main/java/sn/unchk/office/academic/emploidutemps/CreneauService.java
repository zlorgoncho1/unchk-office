package sn.unchk.office.academic.emploidutemps;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.academic.emploidutemps.dto.CreneauCreationDto;
import sn.unchk.office.academic.formation.FormationService;
import sn.unchk.office.common.audit.AuditLogger;

import java.util.List;
import java.util.UUID;

/**
 * Logique des créneaux d'emploi du temps : construction et consultation.
 * <p>
 * Vérifie les règles métier (récurrence exclusive jour/date, horaires cohérents) avant
 * de persister. Le créneau est rattaché à une formation qui doit exister.
 */
@Service
public class CreneauService {

    private final CreneauRepository creneauRepository;
    private final FormationService formationService;
    private final AuditLogger auditLogger;

    public CreneauService(CreneauRepository creneauRepository,
                          FormationService formationService,
                          AuditLogger auditLogger) {
        this.creneauRepository = creneauRepository;
        this.formationService = formationService;
        this.auditLogger = auditLogger;
    }

    /** Liste les créneaux d'une formation (vérifie d'abord l'existence de la formation). */
    @Transactional(readOnly = true)
    public List<Creneau> listerParFormation(UUID formationId) {
        formationService.obtenir(formationId);
        return creneauRepository.findByFormationId(formationId);
    }

    /**
     * Ajoute un créneau à l'emploi du temps d'une formation.
     *
     * @param formationId formation cible (doit exister)
     * @param dto         données validées
     */
    @Transactional
    public Creneau ajouter(UUID formationId, CreneauCreationDto dto) {
        // Vérifie l'existence de la formation (404 sinon).
        formationService.obtenir(formationId);
        verifierCoherence(dto);

        Creneau creneau = new Creneau();
        creneau.setFormationId(formationId);
        creneau.setCourseLabel(dto.courseLabel());
        creneau.setFormateurRef(dto.formateurRef());
        creneau.setDayOfWeek(dto.dayOfWeek());
        creneau.setSessionDate(dto.sessionDate());
        creneau.setStartTime(dto.startTime());
        creneau.setEndTime(dto.endTime());
        creneau.setRoom(dto.room());

        Creneau enregistre = creneauRepository.save(creneau);
        auditLogger.succes("AJOUT_CRENEAU", "formation", formationId.toString());
        return enregistre;
    }

    /**
     * Supprime un créneau d'emploi du temps.
     */
    @Transactional
    public void supprimer(UUID formationId, UUID creneauId) {
        formationService.obtenir(formationId);
        creneauRepository.deleteById(creneauId);
        auditLogger.succes("SUPPRESSION_CRENEAU", "formation", formationId.toString());
    }

    /**
     * Vérifie les contraintes du DDL : récurrence exclusive (jour XOR date) et horaires cohérents.
     */
    private void verifierCoherence(CreneauCreationDto dto) {
        boolean recurrent = dto.dayOfWeek() != null;
        boolean ponctuel = dto.sessionDate() != null;
        if (recurrent == ponctuel) {
            // Ni l'un ni l'autre, ou les deux : viole le CHECK (day_of_week XOR session_date).
            throw new IllegalArgumentException(
                    "Un créneau doit être soit récurrent (jour de la semaine), soit ponctuel (date), mais pas les deux.");
        }
        if (!dto.endTime().isAfter(dto.startTime())) {
            throw new IllegalArgumentException("L'heure de fin doit être postérieure à l'heure de début.");
        }
    }
}
