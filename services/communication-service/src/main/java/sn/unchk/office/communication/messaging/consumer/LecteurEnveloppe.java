package sn.unchk.office.communication.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import sn.unchk.office.common.messaging.DomainEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utilitaires d'extraction des informations d'un message Kafka consommé.
 * <p>
 * L'enveloppe métier vit dans les en-têtes Kafka (eventId, eventType, traceId...). La valeur
 * désérialisée est un {@link DomainEvent} dont le {@code payload} est un arbre JSON générique
 * (map) que l'on convertit vers le type attendu par chaque projection.
 */
public final class LecteurEnveloppe {

    private LecteurEnveloppe() {
        // Classe utilitaire.
    }

    /** Lit l'en-tête {@code eventId} (idempotence) ; à défaut, l'eventId de l'enveloppe. */
    public static UUID eventId(Headers entetes, DomainEvent<?> enveloppe) {
        String depuisEntete = lireEntete(entetes, "eventId");
        if (depuisEntete != null) {
            return UUID.fromString(depuisEntete);
        }
        return enveloppe != null ? enveloppe.eventId() : null;
    }

    /** Lit l'en-tête {@code eventType} ; à défaut, l'eventType de l'enveloppe. */
    public static String eventType(Headers entetes, DomainEvent<?> enveloppe) {
        String depuisEntete = lireEntete(entetes, "eventType");
        if (depuisEntete != null) {
            return depuisEntete;
        }
        return enveloppe != null ? enveloppe.eventType() : null;
    }

    /** Convertit le payload générique de l'enveloppe vers le type cible. */
    public static <T> T payload(DomainEvent<?> enveloppe, Class<T> type, ObjectMapper mapper) {
        if (enveloppe == null || enveloppe.payload() == null) {
            return null;
        }
        return mapper.convertValue(enveloppe.payload(), type);
    }

    /** Lit la valeur d'un en-tête Kafka en UTF-8, ou {@code null} s'il est absent. */
    public static String lireEntete(Headers entetes, String cle) {
        if (entetes == null) {
            return null;
        }
        Header header = entetes.lastHeader(cle);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
