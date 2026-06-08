package sn.unchk.office.identity.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.identity.domaine.EvenementTraite;

import java.util.UUID;

/**
 * Accès au registre des évènements Kafka déjà traités (idempotence des consommateurs).
 */
public interface EvenementTraiteRepository extends JpaRepository<EvenementTraite, UUID> {
}
