package sn.unchk.office.identity.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.identity.domaine.RoleUtilisateur;
import sn.unchk.office.identity.domaine.RoleUtilisateurId;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux affectations de rôles des utilisateurs.
 */
public interface RoleUtilisateurRepository extends JpaRepository<RoleUtilisateur, RoleUtilisateurId> {

    /** Liste les rôles affectés à un utilisateur. */
    List<RoleUtilisateur> findByIdUserId(UUID userId);

    /** Supprime toutes les affectations de rôles d'un utilisateur (avant réécriture). */
    @Transactional
    void deleteByIdUserId(UUID userId);
}
