package sn.unchk.office.identity.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.identity.domaine.CleSignature;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux clés de signature JWT (JWKS + rotation).
 */
public interface CleSignatureRepository extends JpaRepository<CleSignature, UUID> {

    /** Récupère la clé active courante (celle qui signe les nouveaux jetons). */
    Optional<CleSignature> findFirstByActiveTrue();

    /** Récupère une clé par son identifiant kid. */
    Optional<CleSignature> findByKid(String kid);

    /** Toutes les clés (actives ou non) servant à exposer le JWKS et valider les anciens jetons. */
    List<CleSignature> findAll();
}
