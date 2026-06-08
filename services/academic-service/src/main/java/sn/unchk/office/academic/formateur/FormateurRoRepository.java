package sn.unchk.office.academic.formateur;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Accès au read-model des formateurs (projection alimentée par people.staff).
 * <p>
 * En lecture pour l'API (afficher les noms) ; l'écriture est réservée au consommateur Kafka.
 */
public interface FormateurRoRepository extends JpaRepository<FormateurRo, UUID> {

    /** Liste les formateurs en activité. */
    List<FormateurRo> findByActiveTrue();
}
