package sn.unchk.office.people.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.people.domain.Staff;
import sn.unchk.office.people.dto.CreerPersonnelRequest;
import sn.unchk.office.people.dto.ModifierPersonnelRequest;
import sn.unchk.office.people.dto.PersonnelResponse;
import sn.unchk.office.people.messaging.producer.PeopleEventPublisher;
import sn.unchk.office.people.repository.StaffRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Service metier du personnel / formateurs canoniques.
 * <p>
 * Persiste dans la base {@code people} ET emet les evenements sur {@code people.staff}
 * pour alimenter les read-models des autres services (academic, communication, admin,
 * insertion) sans aucun appel REST.
 */
@Service
public class StaffService {

    private final StaffRepository repository;
    private final PeopleEventPublisher publisher;
    private final AuditLogger audit;

    public StaffService(StaffRepository repository,
                        PeopleEventPublisher publisher,
                        AuditLogger audit) {
        this.repository = repository;
        this.publisher = publisher;
        this.audit = audit;
    }

    /** Liste paginee du personnel actif. */
    @Transactional(readOnly = true)
    public Page<PersonnelResponse> lister(Pageable pageable) {
        return repository.findTousActifs(pageable).map(PersonnelResponse::depuis);
    }

    /** Consulte un membre du personnel ; 404 s'il est inconnu/supprime. */
    @Transactional(readOnly = true)
    public PersonnelResponse consulter(UUID id) {
        return PersonnelResponse.depuis(chargerActif(id));
    }

    /** Cree un membre du personnel, puis publie l'evenement de creation. */
    @Transactional
    public PersonnelResponse creer(CreerPersonnelRequest requete, UUID auteur) {
        if (requete.matricule() != null && repository.existsByMatricule(requete.matricule())) {
            throw new ConflitDonneesException("Un membre du personnel avec ce matricule existe deja.");
        }

        Staff staff = new Staff();
        staff.setCreatedBy(auteur);
        staff.setMatricule(requete.matricule());
        staff.setFirstName(requete.firstName());
        staff.setLastName(requete.lastName());
        staff.setGender(requete.gender());
        staff.setKind(requete.kind());
        staff.setEmail(requete.email());
        staff.setPhone(requete.phone());
        staff.setGrade(requete.grade());
        staff.setSpeciality(requete.speciality());
        staff.setDepartment(requete.department());
        staff.setPhotoObjectKey(requete.photoObjectKey());
        staff.setActive(requete.active() == null || requete.active());
        staff.setHiredAt(requete.hiredAt());

        Staff sauve = repository.save(staff);
        publisher.publierPersonnelCree(sauve);
        audit.succes("CREATION_PERSONNEL", "personnel", sauve.getId().toString());
        return PersonnelResponse.depuis(sauve);
    }

    /** Met a jour un membre du personnel, puis publie l'evenement de modification. */
    @Transactional
    public PersonnelResponse modifier(UUID id, ModifierPersonnelRequest requete) {
        Staff staff = chargerActif(id);

        if (requete.matricule() != null
                && !requete.matricule().equals(staff.getMatricule())
                && repository.existsByMatricule(requete.matricule())) {
            throw new ConflitDonneesException("Un membre du personnel avec ce matricule existe deja.");
        }

        staff.setMatricule(requete.matricule());
        staff.setFirstName(requete.firstName());
        staff.setLastName(requete.lastName());
        staff.setGender(requete.gender());
        staff.setKind(requete.kind());
        staff.setEmail(requete.email());
        staff.setPhone(requete.phone());
        staff.setGrade(requete.grade());
        staff.setSpeciality(requete.speciality());
        staff.setDepartment(requete.department());
        staff.setPhotoObjectKey(requete.photoObjectKey());
        staff.setActive(Boolean.TRUE.equals(requete.active()));
        staff.setHiredAt(requete.hiredAt());

        Staff sauve = repository.save(staff);
        publisher.publierPersonnelModifie(sauve);
        audit.succes("MODIFICATION_PERSONNEL", "personnel", sauve.getId().toString());
        return PersonnelResponse.depuis(sauve);
    }

    /** Suppression logique d'un personnel, puis publication d'un tombstone. */
    @Transactional
    public void supprimer(UUID id, UUID auteur) {
        Staff staff = chargerActif(id);
        staff.setDeletedAt(Instant.now());
        staff.setActive(false);
        repository.save(staff);
        publisher.publierPersonnelSupprime(id, auteur);
        audit.succes("SUPPRESSION_PERSONNEL", "personnel", id.toString());
    }

    private Staff chargerActif(UUID id) {
        return repository.findActifById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Personnel introuvable."));
    }
}
