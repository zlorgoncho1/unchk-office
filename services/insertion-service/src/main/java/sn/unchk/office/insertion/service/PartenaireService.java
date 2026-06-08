package sn.unchk.office.insertion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.insertion.domain.Partner;
import sn.unchk.office.insertion.domain.PartnerKind;
import sn.unchk.office.insertion.dto.PartnerRequest;
import sn.unchk.office.insertion.messaging.EvenementInsertion;
import sn.unchk.office.insertion.messaging.PartenairePayload;
import sn.unchk.office.insertion.messaging.ProducteurInsertion;
import sn.unchk.office.insertion.repository.PartnerRepository;
import sn.unchk.office.insertion.web.RessourceIntrouvableException;
import sn.unchk.office.insertion.web.UtilisateurCourant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Logique métier des partenaires (base de données partenaires).
 * <p>
 * Chaque écriture en base est suivie de la publication d'un événement sur
 * {@code insertion.events} (communication 100% Kafka).
 */
@Service
public class PartenaireService {

    private final PartnerRepository depot;
    private final ProducteurInsertion producteur;
    private final AuditLogger audit;

    public PartenaireService(PartnerRepository depot,
                             ProducteurInsertion producteur,
                             AuditLogger audit) {
        this.depot = depot;
        this.producteur = producteur;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Partner> lister() {
        return depot.findByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public Partner consulter(UUID id) {
        return depot.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Partenaire introuvable : " + id));
    }

    @Transactional
    public Partner creer(PartnerRequest requete) {
        Partner p = new Partner();
        appliquer(p, requete);
        p.setCreatedBy(UtilisateurCourant.id());
        Partner enregistre = depot.save(p);

        producteur.publier(enregistre.getId().toString(),
                EvenementInsertion.PARTENAIRE_CREE,
                PartenairePayload.depuis(enregistre));
        audit.succes("CREATION_PARTENAIRE", "partenaire", enregistre.getId().toString());
        return enregistre;
    }

    @Transactional
    public Partner modifier(UUID id, PartnerRequest requete) {
        Partner p = consulter(id);
        appliquer(p, requete);
        Partner enregistre = depot.save(p);

        producteur.publier(enregistre.getId().toString(),
                EvenementInsertion.PARTENAIRE_MODIFIE,
                PartenairePayload.depuis(enregistre));
        audit.succes("MODIFICATION_PARTENAIRE", "partenaire", id.toString());
        return enregistre;
    }

    @Transactional
    public void supprimer(UUID id) {
        Partner p = consulter(id);
        // Suppression logique (auditable).
        p.setDeletedAt(OffsetDateTime.now());
        p.setActive(false);
        depot.save(p);

        producteur.publier(id.toString(),
                EvenementInsertion.PARTENAIRE_SUPPRIME,
                PartenairePayload.depuis(p));
        audit.succes("SUPPRESSION_PARTENAIRE", "partenaire", id.toString());
    }

    /** Recopie les champs autorisés du DTO vers l'entité (mapping explicite, anti sur-affectation). */
    private void appliquer(Partner p, PartnerRequest r) {
        p.setName(r.name());
        p.setKind(r.kind() != null ? r.kind() : PartnerKind.entreprise);
        p.setSector(r.sector());
        p.setContactName(r.contactName());
        p.setContactEmail(r.contactEmail());
        p.setContactPhone(r.contactPhone());
        p.setAddress(r.address());
        p.setCity(r.city());
        p.setActive(r.active() == null || r.active());
    }
}
