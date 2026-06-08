package sn.unchk.office.insertion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.insertion.domain.ContactLog;
import sn.unchk.office.insertion.dto.ContactLogRequest;
import sn.unchk.office.insertion.messaging.ContactPayload;
import sn.unchk.office.insertion.messaging.EvenementInsertion;
import sn.unchk.office.insertion.messaging.ProducteurInsertion;
import sn.unchk.office.insertion.repository.ContactLogRepository;
import sn.unchk.office.insertion.repository.PeopleStudentRoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Logique métier du registre de contact (suivi des diplômés).
 */
@Service
public class ContactService {

    private final ContactLogRepository depot;
    private final PeopleStudentRoRepository etudiants;
    private final ProducteurInsertion producteur;
    private final AuditLogger audit;

    public ContactService(ContactLogRepository depot,
                          PeopleStudentRoRepository etudiants,
                          ProducteurInsertion producteur,
                          AuditLogger audit) {
        this.depot = depot;
        this.etudiants = etudiants;
        this.producteur = producteur;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ContactLog> historique(UUID studentRef) {
        return depot.findByStudentRefOrderByContactedAtDesc(studentRef);
    }

    @Transactional
    public ContactLog enregistrer(ContactLogRequest requete) {
        if (requete.studentRef() == null || !etudiants.existsById(requete.studentRef())) {
            throw new IllegalArgumentException("Étudiant inconnu dans la projection locale.");
        }
        ContactLog c = new ContactLog();
        c.setStudentRef(requete.studentRef());
        c.setContactedAt(requete.contactedAt() != null ? requete.contactedAt() : LocalDate.now());
        c.setChannel(requete.channel());
        c.setNotes(requete.notes());
        c.setAgentRef(requete.agentRef());
        ContactLog enregistre = depot.save(c);

        producteur.publier(enregistre.getStudentRef().toString(),
                EvenementInsertion.CONTACT_ENREGISTRE,
                ContactPayload.depuis(enregistre));
        audit.succes("ENREGISTREMENT_CONTACT", "contact", enregistre.getId().toString());
        return enregistre;
    }
}
