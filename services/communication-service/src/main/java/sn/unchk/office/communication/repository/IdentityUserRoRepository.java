package sn.unchk.office.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.unchk.office.communication.projection.IdentityUserRo;

import java.util.List;
import java.util.UUID;

/**
 * Accès au read-model des utilisateurs (projection {@code identity.users}).
 * <p>
 * Utilisé pour résoudre les destinataires d'une notification par rôle, sans appel REST.
 */
public interface IdentityUserRoRepository extends JpaRepository<IdentityUserRo, UUID> {

    /**
     * Utilisateurs actifs dont au moins un rôle figure dans la liste demandée.
     * <p>
     * La requête s'appuie sur l'opérateur de chevauchement de tableaux PostgreSQL
     * ({@code &&}) sur la colonne {@code roles} (index GIN). Le paramètre est passé sous
     * forme de tableau {@code text[]} pour rester paramétré (anti-injection).
     */
    @Query(value = """
            SELECT * FROM identity_user_ro u
            WHERE u.is_active = TRUE
              AND u.roles && CAST(:roles AS text[])
            """, nativeQuery = true)
    List<IdentityUserRo> trouverActifsParRoles(@Param("roles") String[] roles);
}
