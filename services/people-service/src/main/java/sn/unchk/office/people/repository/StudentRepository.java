package sn.unchk.office.people.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.unchk.office.people.domain.Student;

import java.util.Optional;
import java.util.UUID;

/**
 * Acces aux etudiants canoniques.
 * <p>
 * Toutes les requetes filtrent les etudiants supprimes logiquement
 * ({@code deletedAt IS NULL}) pour ne jamais exposer une fiche archivee.
 */
public interface StudentRepository extends JpaRepository<Student, UUID> {

    /** Recherche d'un etudiant actif par son identifiant. */
    @Query("SELECT s FROM Student s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Student> findActifById(@Param("id") UUID id);

    /** Liste paginee des etudiants actifs. */
    @Query("SELECT s FROM Student s WHERE s.deletedAt IS NULL")
    Page<Student> findTousActifs(Pageable pageable);

    /**
     * Resolution de la fiche "me" : retrouve l'etudiant lie au compte utilisateur courant.
     * Le lien {@code userRef} est resolu cote serveur, jamais via un {@code id} fourni au client.
     */
    @Query("SELECT s FROM Student s WHERE s.userRef = :userRef AND s.deletedAt IS NULL")
    Optional<Student> findActifByUserRef(@Param("userRef") UUID userRef);

    boolean existsByIneIgnoreCase(String ine);

    boolean existsByMatricule(String matricule);
}
