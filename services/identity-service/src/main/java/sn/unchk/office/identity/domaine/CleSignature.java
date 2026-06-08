package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Clé de signature JWT (table {@code signing_keys}).
 * <p>
 * Une paire RSA dont la partie publique ({@code public_pem}) est exposée via
 * {@code /.well-known/jwks.json} et la partie privée ({@code private_pem}) reste secrète
 * (jamais exposée, jamais publiée sur Kafka). Une seule clé est active à la fois pour signer ;
 * la rotation conserve les anciennes clés publiques le temps que les jetons en cours expirent.
 */
@Entity
@Table(name = "signing_keys")
public class CleSignature {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Identifiant de clé (kid) présent dans l'en-tête du JWT, sert à retrouver la clé publique. */
    @Column(name = "kid", nullable = false, unique = true)
    private String kid;

    @Column(name = "algorithm", nullable = false)
    private String algorithm = "RS256";

    /** Clé publique au format PEM, exposée via JWKS. */
    @Column(name = "public_pem", nullable = false)
    private String publicPem;

    /** Clé privée au format PEM : SECRET, jamais exposée. */
    @Column(name = "private_pem", nullable = false)
    private String privatePem;

    /** Vrai pour la clé courante utilisée pour signer (une seule à la fois). */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CleSignature() {
        // Requis par JPA.
    }

    /**
     * Crée une clé de signature active.
     *
     * @param kid        identifiant de clé
     * @param publicPem  clé publique PEM
     * @param privatePem clé privée PEM
     */
    public static CleSignature creer(String kid, String publicPem, String privatePem) {
        CleSignature cle = new CleSignature();
        cle.id = UUID.randomUUID();
        cle.kid = kid;
        cle.algorithm = "RS256";
        cle.publicPem = publicPem;
        cle.privatePem = privatePem;
        cle.active = true;
        cle.createdAt = Instant.now();
        return cle;
    }

    /** Désactive la clé (rotation) : elle ne sert plus à signer mais reste publiée pour la validation. */
    public void desactiver() {
        this.active = false;
        this.rotatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getKid() {
        return kid;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPublicPem() {
        return publicPem;
    }

    public String getPrivatePem() {
        return privatePem;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
