package sn.unchk.office.people.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.people.domain.Student;
import sn.unchk.office.people.domain.StudentDiploma;
import sn.unchk.office.people.dto.CreerEtudiantRequest;
import sn.unchk.office.people.dto.DiplomeDto;
import sn.unchk.office.people.dto.EtudiantResponse;
import sn.unchk.office.people.dto.ModifierEtudiantRequest;
import sn.unchk.office.people.messaging.producer.PeopleEventPublisher;
import sn.unchk.office.people.repository.StudentRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service metier des etudiants canoniques.
 * <p>
 * Gere le cycle de vie (creation, modification, suppression logique), persiste dans
 * la base {@code people} ET emet l'evenement correspondant sur {@code people.students}
 * pour que les autres services mettent a jour leurs read-models (zero REST inter-service).
 */
@Service
public class StudentService {

    private final StudentRepository repository;
    private final PeopleEventPublisher publisher;
    private final AuditLogger audit;

    public StudentService(StudentRepository repository,
                          PeopleEventPublisher publisher,
                          AuditLogger audit) {
        this.repository = repository;
        this.publisher = publisher;
        this.audit = audit;
    }

    /** Liste paginee des etudiants actifs. */
    @Transactional(readOnly = true)
    public Page<EtudiantResponse> lister(Pageable pageable) {
        return repository.findTousActifs(pageable).map(EtudiantResponse::depuis);
    }

    /** Consulte un etudiant par son identifiant ; 404 s'il est inconnu/supprime. */
    @Transactional(readOnly = true)
    public EtudiantResponse consulter(UUID id) {
        return EtudiantResponse.depuis(chargerActif(id));
    }

    /**
     * Resout et renvoie la fiche de l'etudiant lie au compte utilisateur courant.
     * Le {@code userRef} est resolu cote serveur (jamais d'{@code id} fourni par le client) :
     * c'est la defense anti-IDOR pour l'endpoint {@code /api/etudiants/me}.
     */
    @Transactional(readOnly = true)
    public EtudiantResponse consulterFicheCompte(UUID userRef) {
        Student etudiant = repository.findActifByUserRef(userRef)
                .orElseThrow(() -> new RessourceIntrouvableException("Aucune fiche etudiant pour ce compte."));
        return EtudiantResponse.depuis(etudiant);
    }

    /** Cree un etudiant, puis publie l'evenement de creation. */
    @Transactional
    public EtudiantResponse creer(CreerEtudiantRequest requete, UUID auteur) {
        if (repository.existsByIneIgnoreCase(requete.ine())) {
            throw new ConflitDonneesException("Un etudiant avec cet INE existe deja.");
        }
        if (requete.matricule() != null && repository.existsByMatricule(requete.matricule())) {
            throw new ConflitDonneesException("Un etudiant avec ce matricule existe deja.");
        }

        Student etudiant = new Student();
        etudiant.setIne(requete.ine());
        etudiant.setCreatedBy(auteur);
        appliquer(etudiant, requete.matricule(), requete.firstName(), requete.lastName(),
                requete.gender(), requete.birthDate(), requete.birthPlace(), requete.email(),
                requete.phone(), requete.address(), requete.photoObjectKey(), requete.formationRef(),
                requete.promotion(), requete.enrollmentYear(), requete.exitYear(),
                requete.otherTrainings(), requete.userRef());
        if (requete.status() != null) {
            etudiant.setStatus(requete.status());
        }
        remplacerDiplomes(etudiant, requete.diplomas());

        Student sauve = repository.save(etudiant);
        publisher.publierEtudiantCree(sauve);
        audit.succes("CREATION_ETUDIANT", "etudiant", sauve.getId().toString());
        return EtudiantResponse.depuis(sauve);
    }

    /** Met a jour un etudiant existant, puis publie l'evenement de modification. */
    @Transactional
    public EtudiantResponse modifier(UUID id, ModifierEtudiantRequest requete) {
        Student etudiant = chargerActif(id);

        if (requete.matricule() != null
                && !requete.matricule().equals(etudiant.getMatricule())
                && repository.existsByMatricule(requete.matricule())) {
            throw new ConflitDonneesException("Un etudiant avec ce matricule existe deja.");
        }

        appliquer(etudiant, requete.matricule(), requete.firstName(), requete.lastName(),
                requete.gender(), requete.birthDate(), requete.birthPlace(), requete.email(),
                requete.phone(), requete.address(), requete.photoObjectKey(), requete.formationRef(),
                requete.promotion(), requete.enrollmentYear(), requete.exitYear(),
                requete.otherTrainings(), requete.userRef());
        etudiant.setStatus(requete.status());
        remplacerDiplomes(etudiant, requete.diplomas());

        Student sauve = repository.save(etudiant);
        publisher.publierEtudiantModifie(sauve);
        audit.succes("MODIFICATION_ETUDIANT", "etudiant", sauve.getId().toString());
        return EtudiantResponse.depuis(sauve);
    }

    /**
     * Suppression logique d'un etudiant ({@code deletedAt}), puis publication d'un tombstone.
     */
    @Transactional
    public void supprimer(UUID id, UUID auteur) {
        Student etudiant = chargerActif(id);
        etudiant.setDeletedAt(Instant.now());
        repository.save(etudiant);
        publisher.publierEtudiantSupprime(id, auteur);
        audit.succes("SUPPRESSION_ETUDIANT", "etudiant", id.toString());
    }

    /** Charge un etudiant actif ou leve 404 (indistinct d'un refus, anti-enumeration). */
    private Student chargerActif(UUID id) {
        return repository.findActifById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Etudiant introuvable."));
    }

    /** Applique les champs modifiables communs (creation et mise a jour). */
    private void appliquer(Student e, String matricule, String firstName, String lastName,
                           sn.unchk.office.people.domain.Genre gender, java.time.LocalDate birthDate,
                           String birthPlace, String email, String phone, String address,
                           String photoObjectKey, UUID formationRef, String promotion,
                           Short enrollmentYear, Short exitYear, String otherTrainings, UUID userRef) {
        e.setMatricule(matricule);
        e.setFirstName(firstName);
        e.setLastName(lastName);
        e.setGender(gender);
        e.setBirthDate(birthDate);
        e.setBirthPlace(birthPlace);
        e.setEmail(email);
        e.setPhone(phone);
        e.setAddress(address);
        e.setPhotoObjectKey(photoObjectKey);
        e.setFormationRef(formationRef);
        e.setPromotion(promotion);
        e.setEnrollmentYear(enrollmentYear);
        e.setExitYear(exitYear);
        e.setOtherTrainings(otherTrainings);
        e.setUserRef(userRef);
    }

    /** Remplace la liste des diplomes par celle fournie (mapping explicite DTO -> entite). */
    private void remplacerDiplomes(Student etudiant, List<DiplomeDto> diplomes) {
        etudiant.getDiplomas().clear();
        if (diplomes == null) {
            return;
        }
        for (DiplomeDto dto : diplomes) {
            StudentDiploma diplome = new StudentDiploma();
            diplome.setLabel(dto.label());
            diplome.setLevel(dto.level());
            diplome.setObtainedAt(dto.obtainedAt());
            etudiant.ajouterDiplome(diplome);
        }
    }
}
