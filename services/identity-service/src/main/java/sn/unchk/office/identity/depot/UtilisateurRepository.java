package sn.unchk.office.identity.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.identity.domaine.Utilisateur;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux comptes utilisateurs.
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    /** Recherche un compte par courriel (insensible à la casse via la colonne CITEXT). */
    Optional<Utilisateur> findByEmail(String email);

    /** Vérifie l'existence d'un compte par courriel (unicité). */
    boolean existsByEmail(String email);
}
