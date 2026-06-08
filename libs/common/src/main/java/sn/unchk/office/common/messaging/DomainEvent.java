package sn.unchk.office.common.messaging;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Enveloppe standard d'un événement de domaine publié sur Kafka.
 * <p>
 * Tous les services échangent leurs événements avec cette enveloppe pour garantir
 * traçabilité et idempotence. Le {@code payload} est typé en générique pour transporter
 * n'importe quelle charge utile (sérialisée en JSON).
 *
 * @param eventId    identifiant unique de l'événement (UUID) — sert à dédoublonner côté consommateur
 * @param eventType  type de l'événement (ex : "EtudiantCree", "DocumentArchive")
 * @param occurredAt instant de survenue de l'événement
 * @param traceId    identifiant de corrélation propagé de bout en bout (lié à X-Correlation-Id)
 * @param payload    charge utile métier de l'événement
 * @param <T>        type de la charge utile
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DomainEvent<T>(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String traceId,
        T payload
) {

    /**
     * Crée un nouvel événement en générant l'identifiant et l'horodatage automatiquement.
     *
     * @param eventType type de l'événement
     * @param traceId   identifiant de corrélation (peut être {@code null} hors contexte web)
     * @param payload   charge utile métier
     * @return enveloppe prête à être publiée
     */
    public static <T> DomainEvent<T> creer(String eventType, String traceId, T payload) {
        return new DomainEvent<>(UUID.randomUUID(), eventType, Instant.now(), traceId, payload);
    }
}
