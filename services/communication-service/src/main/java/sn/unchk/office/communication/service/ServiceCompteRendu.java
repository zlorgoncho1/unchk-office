package sn.unchk.office.communication.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.communication.domain.CompteRendu;
import sn.unchk.office.communication.dto.CompteRenduCreationRequest;
import sn.unchk.office.communication.dto.CompteRenduDto;
import sn.unchk.office.communication.messaging.payload.CompteRenduEvent;
import sn.unchk.office.communication.messaging.producer.EnregistreurEvenement;
import sn.unchk.office.communication.projection.PeopleStaffRo;
import sn.unchk.office.communication.repository.CompteRenduRepository;
import sn.unchk.office.communication.repository.PeopleStaffRoRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage des comptes rendus : rédaction (brouillon), publication, consultation, liste.
 * <p>
 * La rédaction émet {@code CompteRenduRedige} ; la publication émet {@code CompteRenduPublie}.
 * C'est ce dernier événement, consommé par le service lui-même, qui déclenche la résolution
 * des destinataires (par visibilité de rôle) et l'envoi des notifications.
 */
@Service
public class ServiceCompteRendu {

    private final CompteRenduRepository compteRenduRepository;
    private final PeopleStaffRoRepository staffRo;
    private final EnregistreurEvenement enregistreur;

    public ServiceCompteRendu(CompteRenduRepository compteRenduRepository,
                              PeopleStaffRoRepository staffRo,
                              EnregistreurEvenement enregistreur) {
        this.compteRenduRepository = compteRenduRepository;
        this.staffRo = staffRo;
        this.enregistreur = enregistreur;
    }

    /**
     * Rédige un compte rendu en brouillon (non publié).
     *
     * @param requete    contenu du compte rendu
     * @param createurId utilisateur courant (propriétaire ABAC)
     */
    @Transactional
    public CompteRenduDto rediger(CompteRenduCreationRequest requete, UUID createurId) {
        CompteRendu cr = new CompteRendu();
        cr.setReunionId(requete.reunionId());
        cr.setTitle(requete.title());
        cr.setType(requete.type());
        cr.setBody(requete.body());
        cr.setDocumentRef(requete.documentRef());
        cr.setMeetingDate(requete.meetingDate());
        cr.setAuthorId(requete.authorId());
        cr.setCreatedBy(createurId);
        cr.setVisibility(new HashSet<>(requete.visibility()));
        cr.setPublished(false);
        cr = compteRenduRepository.save(cr);

        // État initial diffusé (utile aux projections : document, admin...).
        enregistreur.enregistrer("CompteRendu", cr.getId(), Topics.COMMUNICATION_COMPTESRENDUS,
                "CompteRenduRedige", CompteRenduEvent.de(cr));

        return versDto(cr);
    }

    /**
     * Publie un compte rendu : déclenche les notifications (via l'événement CompteRenduPublie).
     *
     * @param id identifiant du compte rendu
     * @throws RessourceIntrouvableException si introuvable
     */
    @Transactional
    public CompteRenduDto publier(UUID id) {
        CompteRendu cr = compteRenduRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte rendu introuvable."));
        if (!cr.isPublished()) {
            cr.setPublished(true);
            cr.setPublishedAt(Instant.now());
            cr = compteRenduRepository.save(cr);
            // L'événement de publication déclenche la résolution des destinataires côté consommateur.
            enregistreur.enregistrer("CompteRendu", cr.getId(), Topics.COMMUNICATION_COMPTESRENDUS,
                    "CompteRenduPublie", CompteRenduEvent.de(cr));
        }
        return versDto(cr);
    }

    /** Liste les comptes rendus non supprimés (les plus récents d'abord). */
    @Transactional(readOnly = true)
    public List<CompteRenduDto> lister() {
        return compteRenduRepository.findByDeletedAtIsNullOrderByMeetingDateDesc()
                .stream()
                .map(this::versDto)
                .toList();
    }

    /**
     * Consulte un compte rendu par identifiant.
     *
     * @throws RessourceIntrouvableException si introuvable
     */
    @Transactional(readOnly = true)
    public CompteRenduDto consulter(UUID id) {
        CompteRendu cr = compteRenduRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte rendu introuvable."));
        return versDto(cr);
    }

    /** Construit le DTO en enrichissant le nom de l'auteur depuis le read-model local. */
    private CompteRenduDto versDto(CompteRendu cr) {
        String authorName = staffRo.findById(cr.getAuthorId())
                .map(PeopleStaffRo::getFullName)
                .orElse(null);
        return CompteRenduDto.de(cr, authorName);
    }
}
