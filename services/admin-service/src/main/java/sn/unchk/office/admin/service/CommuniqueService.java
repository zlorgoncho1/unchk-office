package sn.unchk.office.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.admin.domain.AdminCommunique;
import sn.unchk.office.admin.domain.AdminDocKind;
import sn.unchk.office.admin.dto.CommuniqueDto;
import sn.unchk.office.admin.dto.CreationCommuniqueDto;
import sn.unchk.office.admin.dto.MajCommuniqueDto;
import sn.unchk.office.admin.mapper.CommuniqueMapper;
import sn.unchk.office.admin.messaging.CommuniqueEventProducer;
import sn.unchk.office.admin.repository.AdminCommuniqueRepository;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.common.authz.ContexteSecurite;
import sn.unchk.office.common.authz.EntreeOpa;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Service métier des communiqués administratifs (notes de service & circulaires).
 * <p>
 * Règles clés :
 * <ul>
 *   <li>Unicité de la référence à la création (sinon 409).</li>
 *   <li>L'auteur ({@code createdBy}, propriétaire ABAC) provient du JWT, jamais du corps client.</li>
 *   <li>L'audience choisie est convertie en rôles destinataires ({@link Audiences}).</li>
 *   <li>La publication fixe {@code published}/{@code publishedAt} et émet un événement Kafka
 *       consommé par communication-service pour notifier les rôles ciblés.</li>
 *   <li>Suppression logique ({@code deletedAt}).</li>
 * </ul>
 */
@Service
public class CommuniqueService {

    private final AdminCommuniqueRepository repository;
    private final CommuniqueMapper mapper;
    private final CommuniqueEventProducer producteur;
    private final AuditLogger audit;

    public CommuniqueService(AdminCommuniqueRepository repository,
                             CommuniqueMapper mapper,
                             CommuniqueEventProducer producteur,
                             AuditLogger audit) {
        this.repository = repository;
        this.mapper = mapper;
        this.producteur = producteur;
        this.audit = audit;
    }

    /** Crée un communiqué (brouillon, non publié). */
    @Transactional
    public CommuniqueDto creer(CreationCommuniqueDto dto) {
        if (dto.reference() != null && !dto.reference().isBlank()
                && repository.existsByReference(dto.reference())) {
            throw new ConflitRessourceException("Un communiqué porte déjà cette référence.");
        }
        AdminCommunique c = new AdminCommunique();
        c.setKind(dto.kind());
        c.setTitle(dto.title());
        c.setBody(dto.body());
        c.setReference(normaliser(dto.reference()));
        c.setIssueDate(dto.issueDate());
        c.setTargets(new LinkedHashSet<>(Audiences.rolesPour(dto.audience())));
        c.setPublished(false);
        c.setCreatedBy(sujetCourant());
        AdminCommunique enregistre = repository.save(c);

        audit.succes("CREATION_COMMUNIQUE", "communique", enregistre.getId().toString());
        return mapper.versDto(enregistre);
    }

    /** Met à jour un communiqué (la nature reste figée). */
    @Transactional
    public CommuniqueDto mettreAJour(UUID id, MajCommuniqueDto dto) {
        AdminCommunique c = chargerOuLever(id);
        c.setTitle(dto.title());
        c.setBody(dto.body());
        c.setReference(normaliser(dto.reference()));
        if (dto.issueDate() != null) {
            c.setIssueDate(dto.issueDate());
        }
        c.setTargets(new LinkedHashSet<>(Audiences.rolesPour(dto.audience())));
        AdminCommunique enregistre = repository.save(c);

        audit.succes("MAJ_COMMUNIQUE", "communique", id.toString());
        return mapper.versDto(enregistre);
    }

    /**
     * Publie un communiqué : fixe l'état publié puis émet l'événement de notification.
     * Re-publier un communiqué déjà publié est sans effet (pas de double notification).
     */
    @Transactional
    public CommuniqueDto publier(UUID id) {
        AdminCommunique c = chargerOuLever(id);
        if (c.isPublished()) {
            return mapper.versDto(c);
        }
        c.setPublished(true);
        c.setPublishedAt(Instant.now());
        AdminCommunique enregistre = repository.save(c);

        // Notification automatique aux rôles ciblés (via communication-service).
        producteur.publier("CommuniquePublie", mapper.versPayload(enregistre));
        audit.succes("PUBLICATION_COMMUNIQUE", "communique", id.toString());
        return mapper.versDto(enregistre);
    }

    /** Supprime logiquement un communiqué (deletedAt). */
    @Transactional
    public void supprimer(UUID id) {
        AdminCommunique c = chargerOuLever(id);
        c.setDeletedAt(Instant.now());
        repository.save(c);
        audit.succes("SUPPRESSION_COMMUNIQUE", "communique", id.toString());
    }

    /** Consulte un communiqué. */
    @Transactional(readOnly = true)
    public CommuniqueDto consulter(UUID id) {
        return mapper.versDto(chargerOuLever(id));
    }

    /** Liste les communiqués actifs, filtrés optionnellement par nature. */
    @Transactional(readOnly = true)
    public List<CommuniqueDto> lister(AdminDocKind kind) {
        List<AdminCommunique> communiques = (kind != null)
                ? repository.findByKindAndDeletedAtIsNullOrderByIssueDateDesc(kind)
                : repository.findByDeletedAtIsNullOrderByIssueDateDesc();
        return communiques.stream().map(mapper::versDto).toList();
    }

    // ----------------------------------------------------------------
    // Internes
    // ----------------------------------------------------------------

    private String normaliser(String reference) {
        return (reference == null || reference.isBlank()) ? null : reference.trim();
    }

    private AdminCommunique chargerOuLever(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Communiqué introuvable."));
    }

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
