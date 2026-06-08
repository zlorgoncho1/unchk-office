package sn.unchk.office.identity.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.identity.domaine.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux jetons de rafraîchissement (révocation / anti-rejeu).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Recherche un jeton par son empreinte (le token brut n'est jamais stocké). */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Liste les jetons d'un utilisateur (pour révocation en masse à la déconnexion). */
    List<RefreshToken> findByUserId(UUID userId);

    /** Supprime tous les jetons d'un utilisateur (ex : suppression de compte). */
    @Transactional
    void deleteByUserId(UUID userId);
}
