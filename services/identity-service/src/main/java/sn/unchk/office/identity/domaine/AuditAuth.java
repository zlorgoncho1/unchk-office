package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

/**
 * Journal d'authentification (table {@code auth_audit}).
 * <p>
 * Traçabilité OWASP A09 : conserve les évènements de connexion/déconnexion/verrouillage
 * avec adresse IP et agent client, sans jamais journaliser de secret ni de mot de passe.
 */
@Entity
@Table(name = "auth_audit")
public class AuditAuth {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Utilisateur concerné (peut être {@code null} : ex. email inconnu lors d'un échec). */
    @Column(name = "user_id")
    private UUID userId;

    /** Type d'évènement : LOGIN_OK, LOGIN_FAIL, LOGOUT, LOCK, REFRESH_OK, REFRESH_FAIL... */
    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "ip_address", columnDefinition = "inet")
    @ColumnTransformer(write = "?::inet")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditAuth() {
        // Requis par JPA.
    }

    /**
     * Crée une ligne de journal d'authentification.
     *
     * @param userId    utilisateur concerné (peut être {@code null})
     * @param event     code de l'évènement
     * @param ipAddress adresse IP source (peut être {@code null})
     * @param userAgent agent client (peut être {@code null})
     */
    public static AuditAuth creer(UUID userId, String event, String ipAddress, String userAgent) {
        AuditAuth audit = new AuditAuth();
        audit.id = UUID.randomUUID();
        audit.userId = userId;
        audit.event = event;
        audit.ipAddress = ipAddress;
        audit.userAgent = userAgent;
        audit.occurredAt = Instant.now();
        return audit;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEvent() {
        return event;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
