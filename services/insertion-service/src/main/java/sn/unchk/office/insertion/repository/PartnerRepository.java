package sn.unchk.office.insertion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.insertion.domain.Partner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux partenaires. Filtre systématiquement les partenaires supprimés logiquement.
 */
public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    /** Liste les partenaires actifs (non supprimés logiquement). */
    List<Partner> findByDeletedAtIsNull();

    /** Récupère un partenaire non supprimé par son identifiant. */
    Optional<Partner> findByIdAndDeletedAtIsNull(UUID id);
}
