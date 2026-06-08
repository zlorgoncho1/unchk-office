package sn.unchk.office.people.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.people.domain.IdentityUserRo;

import java.util.UUID;

/**
 * Acces au read-model des comptes utilisateurs (projection de {@code identity.users}).
 * <p>
 * Lecture seule cote metier : seul le consommateur Kafka ecrit dans cette table.
 */
public interface IdentityUserRoRepository extends JpaRepository<IdentityUserRo, UUID> {
}
