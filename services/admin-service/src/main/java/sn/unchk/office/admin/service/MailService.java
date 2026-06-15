package sn.unchk.office.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.admin.domain.Mail;
import sn.unchk.office.admin.domain.MailDirection;
import sn.unchk.office.admin.domain.MailStatus;
import sn.unchk.office.admin.dto.ChangementStatutMailDto;
import sn.unchk.office.admin.dto.CreationMailDto;
import sn.unchk.office.admin.dto.MailDto;
import sn.unchk.office.admin.dto.MajMailDto;
import sn.unchk.office.admin.mapper.MailMapper;
import sn.unchk.office.admin.repository.MailRepository;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.common.authz.ContexteSecurite;
import sn.unchk.office.common.authz.EntreeOpa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service métier du registre du courrier (arrivé / départ).
 * <p>
 * Règles clés :
 * <ul>
 *   <li>Unicité de la référence à la création (sinon 409).</li>
 *   <li>L'auteur ({@code createdBy}, propriétaire ABAC) provient du JWT, jamais du corps client.</li>
 *   <li>Le sens du courrier est figé après enregistrement ; le statut suit un endpoint dédié.</li>
 *   <li>Suppression logique ({@code deletedAt}) : aucun courrier n'est effacé physiquement.</li>
 * </ul>
 * Le courrier est une donnée locale au service Administration : aucune propagation Kafka n'est
 * nécessaire (aucun autre service n'en tient de read-model).
 */
@Service
public class MailService {

    private final MailRepository mailRepository;
    private final MailMapper mapper;
    private final AuditLogger audit;

    public MailService(MailRepository mailRepository, MailMapper mapper, AuditLogger audit) {
        this.mailRepository = mailRepository;
        this.mapper = mapper;
        this.audit = audit;
    }

    /** Enregistre un courrier au registre. */
    @Transactional
    public MailDto creer(CreationMailDto dto) {
        if (dto.reference() != null && !dto.reference().isBlank()
                && mailRepository.existsByReference(dto.reference())) {
            throw new ConflitRessourceException("Un courrier porte déjà cette référence.");
        }
        Mail mail = new Mail();
        mail.setDirection(dto.direction());
        mail.setSubject(dto.subject());
        mail.setCorrespondent(dto.correspondent());
        mail.setMailDate(dto.mailDate());
        mail.setStatus(dto.status() != null ? dto.status() : MailStatus.recu);
        mail.setAssignedTo(dto.assignedTo());
        mail.setReference(normaliser(dto.reference()));
        mail.setNotes(dto.notes());
        // Auteur = propriétaire ABAC, issu du JWT (anti sur-affectation / anti-IDOR).
        mail.setCreatedBy(sujetCourant());
        Mail enregistre = mailRepository.save(mail);

        audit.succes("CREATION_COURRIER", "courrier", enregistre.getId().toString());
        return mapper.versDto(enregistre);
    }

    /** Met à jour les attributs modifiables d'un courrier. */
    @Transactional
    public MailDto mettreAJour(UUID id, MajMailDto dto) {
        Mail mail = chargerOuLever(id);
        mail.setSubject(dto.subject());
        mail.setCorrespondent(dto.correspondent());
        mail.setMailDate(dto.mailDate());
        mail.setAssignedTo(dto.assignedTo());
        mail.setReference(normaliser(dto.reference()));
        mail.setNotes(dto.notes());
        Mail enregistre = mailRepository.save(mail);

        audit.succes("MAJ_COURRIER", "courrier", id.toString());
        return mapper.versDto(enregistre);
    }

    /** Fait évoluer le statut d'un courrier (reçu → en traitement → traité → archivé/clos). */
    @Transactional
    public MailDto changerStatut(UUID id, ChangementStatutMailDto dto) {
        Mail mail = chargerOuLever(id);
        mail.setStatus(dto.status());
        Mail enregistre = mailRepository.save(mail);

        audit.succes("CHANGEMENT_STATUT_COURRIER", "courrier", id.toString());
        return mapper.versDto(enregistre);
    }

    /** Supprime logiquement un courrier (deletedAt). */
    @Transactional
    public void supprimer(UUID id) {
        Mail mail = chargerOuLever(id);
        mail.setDeletedAt(Instant.now());
        mailRepository.save(mail);
        audit.succes("SUPPRESSION_COURRIER", "courrier", id.toString());
    }

    /** Consulte un courrier. */
    @Transactional(readOnly = true)
    public MailDto consulter(UUID id) {
        return mapper.versDto(chargerOuLever(id));
    }

    /**
     * Liste les courriers actifs, filtrés optionnellement par sens et/ou statut.
     *
     * @param direction sens du courrier (null = tous)
     * @param status    statut de traitement (null = tous)
     */
    @Transactional(readOnly = true)
    public List<MailDto> lister(MailDirection direction, MailStatus status) {
        List<Mail> courriers = (direction != null)
                ? mailRepository.findByDirectionAndDeletedAtIsNullOrderByMailDateDesc(direction)
                : mailRepository.findByDeletedAtIsNullOrderByMailDateDesc();
        return courriers.stream()
                .filter(m -> status == null || m.getStatus() == status)
                .map(mapper::versDto)
                .toList();
    }

    // ----------------------------------------------------------------
    // Internes
    // ----------------------------------------------------------------

    /** Normalise une référence : null/blanc → null. */
    private String normaliser(String reference) {
        return (reference == null || reference.isBlank()) ? null : reference.trim();
    }

    /** Charge un courrier actif ou lève une 404 (anti-énumération). */
    private Mail chargerOuLever(UUID id) {
        return mailRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Courrier introuvable."));
    }

    /** UUID du sujet courant (claim sub) ou {@code null} hors contexte authentifié. */
    private UUID sujetCourant() {
        EntreeOpa.Sujet sujet = ContexteSecurite.sujetCourant();
        if (sujet.id() == null) {
            return null;
        }
        try {
            return UUID.fromString(sujet.id());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
