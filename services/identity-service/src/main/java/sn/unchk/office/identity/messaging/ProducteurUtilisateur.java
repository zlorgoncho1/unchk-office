package sn.unchk.office.identity.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import sn.unchk.office.common.messaging.DomainEvent;
import sn.unchk.office.common.messaging.Topics;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Producteur Kafka du topic {@code identity.users}.
 * <p>
 * Émis à chaque changement de compte ou de rôle. La clé de partition est l'UUID du compte
 * (ordre garanti par utilisateur). La valeur est l'enveloppe {@link DomainEvent} portant
 * l'état du compte ; l'enveloppe d'évènement (eventId, eventType, aggregate...) est aussi
 * recopiée dans les en-têtes Kafka, conformément à l'architecture.
 */
@Component
public class ProducteurUtilisateur {

    /** Service producteur, repris dans l'en-tête {@code producer}. */
    private static final String PRODUCTEUR = "identity-service";

    /** Type d'agrégat publié. */
    private static final String TYPE_AGREGAT = "User";

    /** Clé MDC de corrélation, alignée avec le filtre web de common. */
    private static final String CLE_CORRELATION = "correlationId";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProducteurUtilisateur(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Publie un évènement de création de compte. */
    public void publierCree(UtilisateurEvenement etat) {
        publier("Created", etat);
    }

    /** Publie un évènement de mise à jour de compte / rôles / statut. */
    public void publierMisAJour(UtilisateurEvenement etat) {
        publier("Updated", etat);
    }

    /** Publie un évènement de suppression (logique) de compte. */
    public void publierSupprime(UtilisateurEvenement etat) {
        publier("Deleted", etat);
    }

    /**
     * Construit l'enveloppe + les en-têtes et envoie le message sur {@code identity.users}.
     */
    private void publier(String typeEvenement, UtilisateurEvenement etat) {
        String traceId = MDC.get(CLE_CORRELATION);
        DomainEvent<UtilisateurEvenement> evenement = DomainEvent.creer(typeEvenement, traceId, etat);
        String cle = etat.userId().toString();

        ProducerRecord<String, Object> message =
                new ProducerRecord<>(Topics.IDENTITY_USERS, cle, evenement);
        remplirEntetes(message.headers(), evenement, etat, typeEvenement, traceId);

        kafkaTemplate.send(message);
    }

    /** Recopie l'enveloppe d'évènement dans les en-têtes Kafka (idempotence, traçabilité, ordre). */
    private void remplirEntetes(Headers entetes, DomainEvent<?> evenement,
                                UtilisateurEvenement etat, String typeEvenement, String traceId) {
        ajouter(entetes, "eventId", evenement.eventId().toString());
        ajouter(entetes, "eventType", typeEvenement);
        ajouter(entetes, "eventVersion", "1");
        ajouter(entetes, "aggregateType", TYPE_AGREGAT);
        ajouter(entetes, "aggregateId", etat.userId().toString());
        ajouter(entetes, "occurredAt", Instant.now().toString());
        ajouter(entetes, "producer", PRODUCTEUR);
        if (traceId != null) {
            ajouter(entetes, "traceId", traceId);
        }
    }

    private void ajouter(Headers entetes, String cle, String valeur) {
        entetes.add(cle, valeur.getBytes(StandardCharsets.UTF_8));
    }
}
