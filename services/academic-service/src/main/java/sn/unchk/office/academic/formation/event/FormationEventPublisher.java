package sn.unchk.office.academic.formation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import sn.unchk.office.academic.formation.Formation;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;

import java.time.Instant;
import java.util.UUID;

/**
 * Producteur Kafka des événements de formation (topic {@code academic.formations}).
 * <p>
 * Émet l'enveloppe {@link DomainEvent} (sérialisée en JSON) avec, pour clé de partition,
 * l'UUID de la formation : l'ordre des événements d'une même formation est ainsi garanti,
 * et la compaction conserve le dernier état par clé. Le {@code traceId} est repris du MDC
 * (posé par le filtre de corrélation) pour la traçabilité de bout en bout.
 */
@Component
public class FormationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FormationEventPublisher.class);

    /** Clé MDC de corrélation, alignée avec le filtre web de la librairie commune. */
    private static final String CLE_CORRELATION = "correlationId";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public FormationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Publie la création d'une formation (eventType = {@code Created}). */
    public void publierCreation(Formation formation) {
        publierEtat("Created", formation);
    }

    /** Publie la mise à jour d'une formation (eventType = {@code Updated}). */
    public void publierMiseAJour(Formation formation) {
        publierEtat("Updated", formation);
    }

    /**
     * Publie la suppression logique d'une formation (eventType = {@code Deleted}).
     * Charge utile « tombstone logique » conforme à l'architecture event-driven.
     */
    public void publierSuppression(UUID formationId, UUID auteur) {
        FormationTombstonePayload tombstone =
                new FormationTombstonePayload(formationId, Instant.now(), auteur);
        DomainEvent<FormationTombstonePayload> evenement =
                DomainEvent.creer("Deleted", traceId(), tombstone);
        envoyer(formationId, evenement, "Deleted");
    }

    /** Construit et publie un événement de transfert d'état pour un type donné. */
    private void publierEtat(String eventType, Formation formation) {
        FormationPayload payload = FormationPayload.de(formation);
        DomainEvent<FormationPayload> evenement = DomainEvent.creer(eventType, traceId(), payload);
        envoyer(formation.getId(), evenement, eventType);
    }

    /** Envoie l'enveloppe sur le topic, clé = UUID de la formation. */
    private void envoyer(UUID cle, DomainEvent<?> evenement, String eventType) {
        kafkaTemplate.send(Topics.ACADEMIC_FORMATIONS, cle.toString(), evenement);
        log.debug("Événement {} publié sur {} pour la formation {}",
                eventType, Topics.ACADEMIC_FORMATIONS, cle);
    }

    /** Récupère l'identifiant de corrélation courant (peut être {@code null} hors contexte web). */
    private String traceId() {
        return MDC.get(CLE_CORRELATION);
    }
}
