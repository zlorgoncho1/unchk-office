package sn.unchk.office.insertion.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.insertion.projection.AcademicFormationRo;
import sn.unchk.office.insertion.projection.PeopleStudentRo;
import sn.unchk.office.insertion.repository.AcademicFormationRoRepository;
import sn.unchk.office.insertion.repository.PeopleStudentRoRepository;
import sn.unchk.office.insertion.repository.ProcessedEventRepository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Consommateurs Kafka alimentant les read-models locaux (CQRS).
 * <p>
 * AUCUN appel REST inter-service : chaque projection est reconstruite uniquement à partir
 * des topics des services propriétaires. Les événements sont dédoublonnés sur {@code eventId}
 * (table {@code processed_events}) pour rester idempotents face à une relivraison Kafka.
 * <p>
 * Le payload arrive sous forme de {@link Map} (désérialisation JSON sans information de type) ;
 * on lit les champs de manière défensive (un champ absent => valeur conservée / par défaut).
 */
@Component
public class ConsommateurProjections {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurProjections.class);

    /** Marqueur de suppression logique (tombstone) : un Deleted porte deletedAt non nul. */
    private static final String TYPE_DELETED = "Deleted";

    private final PeopleStudentRoRepository etudiants;
    private final AcademicFormationRoRepository formations;
    private final ProcessedEventRepository evenementsTraites;

    public ConsommateurProjections(PeopleStudentRoRepository etudiants,
                                   AcademicFormationRoRepository formations,
                                   ProcessedEventRepository evenementsTraites) {
        this.etudiants = etudiants;
        this.formations = formations;
        this.evenementsTraites = evenementsTraites;
    }

    /**
     * Projette les étudiants depuis {@code people.students} vers {@code people_student_ro}.
     */
    @KafkaListener(topics = Topics.PEOPLE_STUDENTS, groupId = "insertion-service-people")
    @Transactional
    public void surEtudiant(@Payload DomainEvent<Map<String, Object>> evenement,
                            @Header(name = KafkaHeaders.OFFSET, required = false) Long offset) {
        if (evenement == null || dejaTraite(evenement, Topics.PEOPLE_STUDENTS)) {
            return;
        }
        Map<String, Object> p = evenement.payload();
        UUID id = lireUuid(p, "id");
        if (id == null) {
            log.warn("Événement people.students sans id : ignoré (eventId={})", evenement.eventId());
            return;
        }

        // Suppression logique : on retire l'étudiant de la projection.
        if (estSuppression(evenement, p)) {
            etudiants.deleteById(id);
            marquerTraite(evenement, Topics.PEOPLE_STUDENTS);
            log.debug("Étudiant retiré de la projection : id={}", id);
            return;
        }

        PeopleStudentRo ro = etudiants.findById(id).orElseGet(PeopleStudentRo::new);
        ro.setId(id);
        ro.setFullName(nomComplet(p));
        ro.setGender(lireTexte(p, "gender", ro.getGender() != null ? ro.getGender() : "inconnu"));
        ro.setFormationRef(lireUuid(p, "formationRef", "formation_ref"));
        ro.setPromotion(lireTexte(p, "promotion", ro.getPromotion()));
        ro.setExitYear(lireShort(p, "exitYear", "exit_year"));
        ro.setLastEventAt(horodatage(evenement));
        ro.setEventOffset(offset);
        etudiants.save(ro);

        marquerTraite(evenement, Topics.PEOPLE_STUDENTS);
        log.debug("Projection étudiant à jour : id={}", id);
    }

    /**
     * Projette les formations depuis {@code academic.formations} vers {@code academic_formation_ro}.
     */
    @KafkaListener(topics = Topics.ACADEMIC_FORMATIONS, groupId = "insertion-service-academic")
    @Transactional
    public void surFormation(@Payload DomainEvent<Map<String, Object>> evenement,
                             @Header(name = KafkaHeaders.OFFSET, required = false) Long offset) {
        if (evenement == null || dejaTraite(evenement, Topics.ACADEMIC_FORMATIONS)) {
            return;
        }
        Map<String, Object> p = evenement.payload();
        UUID id = lireUuid(p, "id");
        if (id == null) {
            log.warn("Événement academic.formations sans id : ignoré (eventId={})", evenement.eventId());
            return;
        }

        if (estSuppression(evenement, p)) {
            formations.deleteById(id);
            marquerTraite(evenement, Topics.ACADEMIC_FORMATIONS);
            return;
        }

        AcademicFormationRo ro = formations.findById(id).orElseGet(AcademicFormationRo::new);
        ro.setId(id);
        ro.setLabel(lireTexte(p, "label", ro.getLabel() != null ? ro.getLabel() : "(sans libellé)"));
        ro.setLevel(lireTexte(p, "level", ro.getLevel() != null ? ro.getLevel() : "inconnu"));
        ro.setLastEventAt(horodatage(evenement));
        ro.setEventOffset(offset);
        formations.save(ro);

        marquerTraite(evenement, Topics.ACADEMIC_FORMATIONS);
        log.debug("Projection formation à jour : id={}", id);
    }

    // --- Idempotence -------------------------------------------------------

    private boolean dejaTraite(DomainEvent<?> evenement, String topic) {
        UUID eventId = evenement.eventId();
        if (eventId != null && evenementsTraites.existsById(eventId)) {
            log.debug("Événement déjà traité, ignoré : eventId={} topic={}", eventId, topic);
            return true;
        }
        return false;
    }

    private void marquerTraite(DomainEvent<?> evenement, String topic) {
        if (evenement.eventId() != null) {
            evenementsTraites.save(new ProcessedEvent(evenement.eventId(), topic));
        }
    }

    // --- Lecture défensive du payload --------------------------------------

    private boolean estSuppression(DomainEvent<?> evenement, Map<String, Object> p) {
        if (evenement.eventType() != null && evenement.eventType().contains(TYPE_DELETED)) {
            return true;
        }
        return p != null && p.get("deletedAt") != null;
    }

    private String nomComplet(Map<String, Object> p) {
        String complet = lireTexte(p, "fullName", null);
        if (complet == null) {
            complet = lireTexte(p, "full_name", null);
        }
        if (complet != null) {
            return complet;
        }
        // Reconstitution depuis prénom + nom si le nom complet n'est pas fourni.
        String prenom = lireTexte(p, "firstName", lireTexte(p, "first_name", ""));
        String nom = lireTexte(p, "lastName", lireTexte(p, "last_name", ""));
        String assemble = (prenom + " " + nom).trim();
        return assemble.isEmpty() ? "(inconnu)" : assemble;
    }

    private String lireTexte(Map<String, Object> p, String cle, String defaut) {
        if (p == null) {
            return defaut;
        }
        Object valeur = p.get(cle);
        return valeur != null ? valeur.toString() : defaut;
    }

    private UUID lireUuid(Map<String, Object> p, String... cles) {
        if (p == null) {
            return null;
        }
        for (String cle : cles) {
            Object valeur = p.get(cle);
            if (valeur != null) {
                try {
                    return UUID.fromString(valeur.toString());
                } catch (IllegalArgumentException ex) {
                    log.warn("UUID invalide pour la clé {} : {}", cle, valeur);
                    return null;
                }
            }
        }
        return null;
    }

    private Short lireShort(Map<String, Object> p, String... cles) {
        if (p == null) {
            return null;
        }
        for (String cle : cles) {
            Object valeur = p.get(cle);
            if (valeur instanceof Number nombre) {
                return nombre.shortValue();
            }
            if (valeur != null) {
                try {
                    return Short.valueOf(valeur.toString());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private OffsetDateTime horodatage(DomainEvent<?> evenement) {
        return evenement.occurredAt() != null
                ? evenement.occurredAt().atOffset(java.time.ZoneOffset.UTC)
                : OffsetDateTime.now();
    }
}
