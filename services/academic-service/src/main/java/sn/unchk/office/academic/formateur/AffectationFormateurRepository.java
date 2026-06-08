package sn.unchk.office.academic.formateur;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux affectations de formateurs (clé composite).
 */
public interface AffectationFormateurRepository
        extends JpaRepository<AffectationFormateur, AffectationFormateurId> {

    /** Liste les affectations d'une formation. */
    List<AffectationFormateur> findByIdFormationId(UUID formationId);
}
