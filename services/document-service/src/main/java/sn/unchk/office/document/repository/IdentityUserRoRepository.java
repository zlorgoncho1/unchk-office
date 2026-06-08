package sn.unchk.office.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.document.domain.IdentityUserRo;

import java.util.UUID;

/**
 * Accès au read-model local des comptes utilisateurs (projection de identity.users).
 */
public interface IdentityUserRoRepository extends JpaRepository<IdentityUserRo, UUID> {
}
