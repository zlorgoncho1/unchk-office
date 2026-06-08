package sn.unchk.office.insertion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sn.unchk.office.insertion.domain.InsertionKind;
import sn.unchk.office.insertion.domain.InsertionOutcome;

import java.util.List;
import java.util.UUID;

/**
 * Accès aux situations d'insertion (support des statistiques).
 */
public interface InsertionOutcomeRepository extends JpaRepository<InsertionOutcome, UUID> {

    List<InsertionOutcome> findByStudentRef(UUID studentRef);

    /** Situations courantes uniquement (la dernière connue par étudiant). */
    List<InsertionOutcome> findByCurrentTrue();

    /**
     * Compte les situations courantes par type d'insertion (auto-emploi vs salarié...).
     * Renvoie des lignes {@code [InsertionKind, Long]}.
     */
    @Query("""
            select o.kind, count(o)
            from InsertionOutcome o
            where o.current = true
            group by o.kind
            """)
    List<Object[]> compterParTypeCourant();

    /**
     * Compte les situations courantes par formation et par type d'insertion.
     * Renvoie des lignes {@code [formationRef, InsertionKind, Long]}.
     */
    @Query("""
            select o.formationRef, o.kind, count(o)
            from InsertionOutcome o
            where o.current = true
            group by o.formationRef, o.kind
            """)
    List<Object[]> compterParFormationEtTypeCourant();
}
