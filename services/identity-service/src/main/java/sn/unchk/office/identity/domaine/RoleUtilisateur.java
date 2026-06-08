package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Affectation d'un rôle à un utilisateur (table {@code user_roles}).
 * <p>
 * Clé composite {@code (user_id, role)} : un utilisateur peut cumuler plusieurs rôles.
 * <p>
 * Implémente {@link Persistable} pour indiquer à Spring Data que les nouvelles affectations
 * sont à INSÉRER directement (sans SELECT préalable d'existence) : cela évite une comparaison
 * SQL {@code role = ?} sur la colonne du type énuméré {@code role_code} (qui nécessiterait un
 * transtypage côté WHERE). L'écriture du rôle, elle, est transtypée via {@code ?::role_code}
 * sur le champ de la clé.
 */
@Entity
@Table(name = "user_roles")
public class RoleUtilisateur implements Persistable<RoleUtilisateurId> {

    @EmbeddedId
    private RoleUtilisateurId id;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    /** Marque l'entité comme « nouvelle » tant qu'elle n'a pas été chargée depuis la base. */
    @Transient
    private boolean nouvelle = false;

    protected RoleUtilisateur() {
        // Requis par JPA.
    }

    /**
     * Crée une affectation de rôle pour un utilisateur.
     *
     * @param userId    identifiant de l'utilisateur
     * @param role      rôle accordé
     * @param grantedBy auteur de l'octroi (peut être {@code null})
     */
    public static RoleUtilisateur creer(UUID userId, RoleCode role, UUID grantedBy) {
        RoleUtilisateur affectation = new RoleUtilisateur();
        affectation.id = new RoleUtilisateurId(userId, role);
        affectation.grantedAt = Instant.now();
        affectation.grantedBy = grantedBy;
        affectation.nouvelle = true;
        return affectation;
    }

    @Override
    public RoleUtilisateurId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return nouvelle;
    }

    /** Après chargement depuis la base, l'entité n'est plus « nouvelle ». */
    @jakarta.persistence.PostLoad
    void marquerCharge() {
        this.nouvelle = false;
    }

    public RoleCode getRole() {
        return id != null ? id.getRole() : null;
    }

    public UUID getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }
}
