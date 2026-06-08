package sn.unchk.office.insertion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.insertion.domain.Internship;
import sn.unchk.office.insertion.domain.InternshipStatus;
import sn.unchk.office.insertion.dto.InternshipRequest;
import sn.unchk.office.insertion.messaging.EvenementInsertion;
import sn.unchk.office.insertion.messaging.ProducteurInsertion;
import sn.unchk.office.insertion.messaging.StagePayload;
import sn.unchk.office.insertion.repository.InternshipRepository;
import sn.unchk.office.insertion.repository.PeopleStudentRoRepository;
import sn.unchk.office.insertion.web.RessourceIntrouvableException;
import sn.unchk.office.insertion.web.UtilisateurCourant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Logique métier des stages (bilans de stages).
 * <p>
 * La référence étudiant est validée contre le read-model local {@code people_student_ro}
 * (alimenté par Kafka) plutôt que par un appel REST vers people-service.
 */
@Service
public class StageService {

    private final InternshipRepository depot;
    private final PeopleStudentRoRepository etudiants;
    private final ProducteurInsertion producteur;
    private final AuditLogger audit;

    public StageService(InternshipRepository depot,
                        PeopleStudentRoRepository etudiants,
                        ProducteurInsertion producteur,
                        AuditLogger audit) {
        this.depot = depot;
        this.etudiants = etudiants;
        this.producteur = producteur;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Internship> lister() {
        return depot.findByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public List<Internship> listerParEtudiant(UUID studentRef) {
        return depot.findByStudentRefAndDeletedAtIsNull(studentRef);
    }

    @Transactional(readOnly = true)
    public Internship consulter(UUID id) {
        return depot.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Stage introuvable : " + id));
    }

    @Transactional
    public Internship creer(InternshipRequest requete) {
        validerEtudiant(requete.studentRef());
        Internship s = new Internship();
        appliquer(s, requete);
        s.setCreatedBy(UtilisateurCourant.id());
        Internship enregistre = depot.save(s);

        // Clé de partition = studentId (cf. docs/architecture.md).
        producteur.publier(enregistre.getStudentRef().toString(),
                EvenementInsertion.STAGE_CREE,
                StagePayload.depuis(enregistre));
        audit.succes("CREATION_STAGE", "stage", enregistre.getId().toString());
        return enregistre;
    }

    @Transactional
    public Internship modifier(UUID id, InternshipRequest requete) {
        validerEtudiant(requete.studentRef());
        Internship s = consulter(id);
        InternshipStatus ancienStatut = s.getStatus();
        appliquer(s, requete);
        Internship enregistre = depot.save(s);

        // Un passage à « valide » correspond à la clôture du bilan de stage.
        boolean valide = enregistre.getStatus() == InternshipStatus.valide
                && ancienStatut != InternshipStatus.valide;
        String type = valide ? EvenementInsertion.STAGE_VALIDE : EvenementInsertion.STAGE_MODIFIE;

        producteur.publier(enregistre.getStudentRef().toString(), type,
                StagePayload.depuis(enregistre));
        audit.succes("MODIFICATION_STAGE", "stage", id.toString());
        return enregistre;
    }

    @Transactional
    public void supprimer(UUID id) {
        Internship s = consulter(id);
        s.setDeletedAt(OffsetDateTime.now());
        depot.save(s);
        audit.succes("SUPPRESSION_STAGE", "stage", id.toString());
    }

    /** Vérifie que l'étudiant référencé existe bien dans la projection locale. */
    private void validerEtudiant(UUID studentRef) {
        if (studentRef == null || !etudiants.existsById(studentRef)) {
            // Référence inconnue localement : on refuse (la projection sera reconstruite par Kafka).
            throw new IllegalArgumentException("Étudiant inconnu dans la projection locale.");
        }
    }

    private void appliquer(Internship s, InternshipRequest r) {
        s.setStudentRef(r.studentRef());
        s.setPartnerId(r.partnerId());
        s.setTitle(r.title());
        s.setStartDate(r.startDate());
        s.setEndDate(r.endDate());
        s.setStatus(r.status() != null ? r.status() : InternshipStatus.prevu);
        s.setTutorRef(r.tutorRef());
        s.setSupervisorName(r.supervisorName());
        s.setReportRef(r.reportRef());
        s.setGrade(r.grade());
    }
}
