package sn.unchk.office.insertion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.insertion.domain.InsertionOutcome;
import sn.unchk.office.insertion.dto.InsertionOutcomeRequest;
import sn.unchk.office.insertion.messaging.EvenementInsertion;
import sn.unchk.office.insertion.messaging.InsertionPayload;
import sn.unchk.office.insertion.messaging.ProducteurInsertion;
import sn.unchk.office.insertion.repository.InsertionOutcomeRepository;
import sn.unchk.office.insertion.repository.PeopleStudentRoRepository;
import sn.unchk.office.insertion.web.RessourceIntrouvableException;
import sn.unchk.office.insertion.web.UtilisateurCourant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Logique métier des situations d'insertion (support des statistiques).
 */
@Service
public class InsertionOutcomeService {

    private final InsertionOutcomeRepository depot;
    private final PeopleStudentRoRepository etudiants;
    private final ProducteurInsertion producteur;
    private final AuditLogger audit;

    public InsertionOutcomeService(InsertionOutcomeRepository depot,
                                   PeopleStudentRoRepository etudiants,
                                   ProducteurInsertion producteur,
                                   AuditLogger audit) {
        this.depot = depot;
        this.etudiants = etudiants;
        this.producteur = producteur;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<InsertionOutcome> listerParEtudiant(UUID studentRef) {
        return depot.findByStudentRef(studentRef);
    }

    @Transactional(readOnly = true)
    public InsertionOutcome consulter(UUID id) {
        return depot.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Situation introuvable : " + id));
    }

    @Transactional
    public InsertionOutcome declarer(InsertionOutcomeRequest requete) {
        if (requete.studentRef() == null || !etudiants.existsById(requete.studentRef())) {
            throw new IllegalArgumentException("Étudiant inconnu dans la projection locale.");
        }
        InsertionOutcome o = new InsertionOutcome();
        appliquer(o, requete);
        o.setCreatedBy(UtilisateurCourant.id());
        InsertionOutcome enregistre = depot.save(o);

        producteur.publier(enregistre.getStudentRef().toString(),
                EvenementInsertion.INSERTION_DECLAREE,
                InsertionPayload.depuis(enregistre));
        audit.succes("DECLARATION_INSERTION", "insertion", enregistre.getId().toString());
        return enregistre;
    }

    @Transactional
    public InsertionOutcome modifier(UUID id, InsertionOutcomeRequest requete) {
        InsertionOutcome o = consulter(id);
        appliquer(o, requete);
        InsertionOutcome enregistre = depot.save(o);

        producteur.publier(enregistre.getStudentRef().toString(),
                EvenementInsertion.INSERTION_MODIFIEE,
                InsertionPayload.depuis(enregistre));
        audit.succes("MODIFICATION_INSERTION", "insertion", id.toString());
        return enregistre;
    }

    private void appliquer(InsertionOutcome o, InsertionOutcomeRequest r) {
        o.setStudentRef(r.studentRef());
        o.setFormationRef(r.formationRef());
        o.setKind(r.kind());
        o.setEmployerName(r.employerName());
        o.setJobTitle(r.jobTitle());
        o.setObservedAt(r.observedAt() != null ? r.observedAt() : LocalDate.now());
        o.setCurrent(r.current() == null || r.current());
    }
}
