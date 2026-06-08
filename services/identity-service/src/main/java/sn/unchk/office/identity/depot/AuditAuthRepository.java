package sn.unchk.office.identity.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.identity.domaine.AuditAuth;

import java.util.UUID;

/**
 * Accès au journal d'authentification (traçabilité OWASP A09).
 */
public interface AuditAuthRepository extends JpaRepository<AuditAuth, UUID> {
}
