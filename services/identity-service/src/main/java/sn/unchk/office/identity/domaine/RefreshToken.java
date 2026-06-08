package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Jeton de rafraîchissement (table {@code refresh_tokens}).
 * <p>
 * Permet de prolonger une session sans renvoyer les identifiants. On ne stocke que le
 * <em>hash</em> du token (jamais le token brut), pour la révocation et l'anti-rejeu.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Empreinte du token (SHA-256) ; le token brut n'est jamais persisté. */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Renseigné quand le token est révoqué (déconnexion / rotation). */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {
        // Requis par JPA.
    }

    /**
     * Crée un jeton de rafraîchissement actif.
     *
     * @param userId    propriétaire
     * @param tokenHash empreinte du token
     * @param expiresAt expiration
     */
    public static RefreshToken creer(UUID userId, String tokenHash, Instant expiresAt) {
        RefreshToken jeton = new RefreshToken();
        jeton.id = UUID.randomUUID();
        jeton.userId = userId;
        jeton.tokenHash = tokenHash;
        jeton.expiresAt = expiresAt;
        jeton.createdAt = Instant.now();
        return jeton;
    }

    /** Révoque le jeton (il ne pourra plus servir à rafraîchir). */
    public void revoquer() {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    /** Indique si le jeton est encore valide (ni expiré, ni révoqué). */
    public boolean estValide() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
