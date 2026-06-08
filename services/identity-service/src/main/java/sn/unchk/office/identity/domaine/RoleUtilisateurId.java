package sn.unchk.office.identity.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.ColumnTransformer;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Clé primaire composite de {@link RoleUtilisateur} : {@code (user_id, role)}.
 * <p>
 * Un utilisateur peut cumuler plusieurs rôles ; chaque couple (utilisateur, rôle) est unique.
 * Le rôle est conservé sous forme de libellé textuel ({@code role_libelle}) mais écrit dans
 * la colonne du type énuméré PostgreSQL {@code role_code} grâce au transtypage {@code ?::role_code}.
 */
@Embeddable
public class RoleUtilisateurId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Libellé canonique du rôle (ex : {@code appui-insertion}). */
    @Column(name = "role", nullable = false)
    @ColumnTransformer(write = "?::role_code")
    private String roleLibelle;

    public RoleUtilisateurId() {
        // Requis par JPA.
    }

    public RoleUtilisateurId(UUID userId, RoleCode role) {
        this.userId = userId;
        this.roleLibelle = role.libelle();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    /** Renvoie le rôle reconstitué depuis son libellé. */
    public RoleCode getRole() {
        return RoleCode.depuisLibelle(roleLibelle);
    }

    public void setRole(RoleCode role) {
        this.roleLibelle = role.libelle();
    }

    public String getRoleLibelle() {
        return roleLibelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleUtilisateurId autre)) {
            return false;
        }
        return Objects.equals(userId, autre.userId) && Objects.equals(roleLibelle, autre.roleLibelle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleLibelle);
    }
}
