package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Compte utilisateur (table {@code users}).
 * <p>
 * Source de vérité de l'identité fédérée maison : login (email), hash de mot de passe,
 * statut d'activation / verrouillage et lien optionnel vers une personne canonique
 * (étudiant ou personnel) du people-service. La clé primaire est un UUID (anti-IDOR).
 * Le hash du mot de passe n'est JAMAIS exposé ni publié sur Kafka.
 */
@Entity
@Table(name = "users")
public class Utilisateur {

    /** Identifiant opaque (UUID v4). */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Login : courriel unique (insensible à la casse côté base via CITEXT). */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Empreinte BCrypt du mot de passe (jamais en clair, jamais exposée). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Nom complet affiché. */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** Référence optionnelle vers people.students.id ou people.staff.id. */
    @Column(name = "person_ref")
    private UUID personRef;

    /** Nature de l'entité liée : {@code etudiant} ou {@code personnel}. */
    @Column(name = "person_kind")
    private String personKind;

    /** Compte activé (true par défaut). */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Compte verrouillé (anti-bruteforce). */
    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    /** Compteur d'échecs de connexion consécutifs. */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    /** Horodatage de la dernière connexion réussie. */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Verrou optimiste. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Suppression logique (le compte n'est pas effacé physiquement). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Utilisateur() {
        // Requis par JPA.
    }

    /**
     * Crée un nouveau compte actif avec un identifiant et des horodatages frais.
     */
    public static Utilisateur creer(String email, String passwordHash, String fullName,
                                    UUID personRef, String personKind) {
        Utilisateur u = new Utilisateur();
        Instant maintenant = Instant.now();
        u.id = UUID.randomUUID();
        u.email = email;
        u.passwordHash = passwordHash;
        u.fullName = fullName;
        u.personRef = personRef;
        u.personKind = personKind;
        u.active = true;
        u.locked = false;
        u.failedAttempts = 0;
        u.createdAt = maintenant;
        u.updatedAt = maintenant;
        return u;
    }

    /** Marque l'instant de modification (à appeler avant toute persistance d'un changement). */
    public void toucher() {
        this.updatedAt = Instant.now();
    }

    /** Enregistre une connexion réussie : remet à zéro le compteur d'échecs. */
    public void connexionReussie() {
        this.lastLoginAt = Instant.now();
        this.failedAttempts = 0;
        toucher();
    }

    /**
     * Enregistre une tentative échouée et verrouille le compte au-delà du seuil.
     *
     * @param maxEchecs nombre d'échecs toléré avant verrouillage
     */
    public void connexionEchouee(int maxEchecs) {
        this.failedAttempts++;
        if (this.failedAttempts >= maxEchecs) {
            this.locked = true;
        }
        toucher();
    }

    /** Indique si le compte est utilisable pour s'authentifier. */
    public boolean estUtilisable() {
        return active && !locked && deletedAt == null;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UUID getPersonRef() {
        return personRef;
    }

    public void setPersonRef(UUID personRef) {
        this.personRef = personRef;
    }

    public String getPersonKind() {
        return personKind;
    }

    public void setPersonKind(String personKind) {
        this.personKind = personKind;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
