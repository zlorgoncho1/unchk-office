package sn.unchk.office.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.unchk.office.admin.domain.Mail;
import sn.unchk.office.admin.domain.MailDirection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accès aux courriers. Requêtes paramétrées par Spring Data (anti-injection SQL).
 * Le filtre {@code deletedAtIsNull} masque les courriers supprimés logiquement.
 */
public interface MailRepository extends JpaRepository<Mail, UUID> {

    /** Liste les courriers actifs (les plus récents d'abord). */
    List<Mail> findByDeletedAtIsNullOrderByMailDateDesc();

    /** Liste les courriers actifs d'un sens donné (arrivé / départ). */
    List<Mail> findByDirectionAndDeletedAtIsNullOrderByMailDateDesc(MailDirection direction);

    /** Charge un courrier actif (non supprimé) par identifiant. */
    Optional<Mail> findByIdAndDeletedAtIsNull(UUID id);

    /** Vérifie l'unicité d'une référence avant création. */
    boolean existsByReference(String reference);
}
