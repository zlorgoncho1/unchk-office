package sn.unchk.office.document.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.common.authz.ContexteSecurite;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.document.config.MinioProprietes;
import sn.unchk.office.document.config.UploadProprietes;
import sn.unchk.office.document.domain.CategorieDocument;
import sn.unchk.office.document.domain.Document;
import sn.unchk.office.document.domain.DocumentShare;
import sn.unchk.office.document.domain.DocumentVisibility;
import sn.unchk.office.document.domain.OutboxMessage;
import sn.unchk.office.document.dto.CreerDocumentRequete;
import sn.unchk.office.document.dto.DocumentEtatEvenement;
import sn.unchk.office.document.dto.DocumentReponse;
import sn.unchk.office.document.dto.DocumentSupprimeEvenement;
import sn.unchk.office.document.dto.MettreAJourDocumentRequete;
import sn.unchk.office.document.dto.PartageRequete;
import sn.unchk.office.document.dto.UrlTelechargementReponse;
import sn.unchk.office.document.messaging.EnteteEvenement;
import sn.unchk.office.document.repository.DocumentRepository;
import sn.unchk.office.document.repository.DocumentShareRepository;
import sn.unchk.office.document.repository.DocumentVisibilityRepository;
import sn.unchk.office.document.repository.OutboxRepository;
import sn.unchk.office.document.storage.StockageObjet;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Logique métier de la gestion documentaire.
 * <p>
 * Orchestration : validation d'upload (taille + MIME), dépôt MinIO, persistance des
 * métadonnées + visibilité, écriture Outbox (publication différée sur {@code document.documents}),
 * lecture, mise à jour, suppression logique, partage et génération d'URLs de téléchargement.
 * <p>
 * Le contrôle d'accès anti-IDOR est porté par l'annotation {@code @VerifieAccesObjet} sur le
 * contrôleur (garde OPA au niveau objet) ; ce service applique en complément les règles métier.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    /** Version du schéma de payload publié (évolutions additives uniquement). */
    private static final int VERSION_EVENEMENT = 1;

    private final DocumentRepository documents;
    private final DocumentVisibilityRepository visibilites;
    private final DocumentShareRepository partages;
    private final OutboxRepository outbox;
    private final StockageObjet stockage;
    private final MinioProprietes minioProprietes;
    private final UploadProprietes uploadProprietes;
    private final ObjectMapper objectMapper;
    private final AuditLogger audit;

    public DocumentService(DocumentRepository documents,
                           DocumentVisibilityRepository visibilites,
                           DocumentShareRepository partages,
                           OutboxRepository outbox,
                           StockageObjet stockage,
                           MinioProprietes minioProprietes,
                           UploadProprietes uploadProprietes,
                           ObjectMapper objectMapper,
                           AuditLogger audit) {
        this.documents = documents;
        this.visibilites = visibilites;
        this.partages = partages;
        this.outbox = outbox;
        this.stockage = stockage;
        this.minioProprietes = minioProprietes;
        this.uploadProprietes = uploadProprietes;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    /**
     * Dépose un nouveau document : binaire dans MinIO, métadonnées + visibilité en base,
     * événement {@code Created} en Outbox. Le tout en une transaction (atomicité base/Kafka).
     *
     * @param requete  métadonnées du document
     * @param fichier  binaire à stocker
     * @param traceId  identifiant de corrélation à propager dans l'événement
     * @return représentation du document créé
     */
    @Transactional
    public DocumentReponse creer(CreerDocumentRequete requete, MultipartFile fichier, String traceId) {
        validerFichier(fichier);

        CategorieDocument categorie = CategorieDocument.depuisCode(requete.category());
        String bucket = bucketPour(categorie);
        // Clé objet non devinable : UUID + nom d'origine nettoyé.
        UUID id = UUID.randomUUID();
        String objectKey = id + "/" + nomFichierSur(fichier.getOriginalFilename());

        // 1) Dépôt du binaire dans MinIO (hors transaction DB, mais avant la persistance).
        try {
            stockage.deposer(bucket, objectKey, fichier.getInputStream(),
                    fichier.getSize(), fichier.getContentType());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Lecture du fichier impossible.");
        }

        UUID acteur = utilisateurCourant();

        // 2) Persistance des métadonnées.
        Document document = new Document();
        document.setId(id);
        document.setTitle(requete.title());
        document.setCategory(categorie);
        document.setDescription(requete.description());
        document.setBucket(bucket);
        document.setObjectKey(objectKey);
        document.setMimeType(fichier.getContentType());
        document.setSizeBytes(fichier.getSize());
        document.setChecksumSha256(calculerChecksum(fichier));
        document.setOwnerId(acteur);
        document.setCreatedBy(acteur);
        document.setSourceService(requete.sourceService());
        document.setSourceRef(requete.sourceRef());
        documents.save(document);

        // 3) Visibilité par rôle (alimente le visibility[] OPA / anti-IDOR).
        List<String> roles = enregistrerVisibilite(id, requete.visibility());

        // 4) Événement Created dans l'Outbox (publié ensuite par le relais).
        ecrireOutbox(document, roles, "Created", traceId);

        audit.succes("CREATION_DOCUMENT", "document", id.toString());
        return DocumentReponse.depuis(document, roles);
    }

    /**
     * Renvoie les métadonnées d'un document actif. La garde OPA (anti-IDOR) est appliquée
     * en amont par le contrôleur.
     */
    @Transactional(readOnly = true)
    public DocumentReponse consulter(UUID id) {
        Document document = chargerActif(id);
        return DocumentReponse.depuis(document, visibilites.rolesAutorises(id));
    }

    /**
     * Liste paginée des documents actifs, filtrable par catégorie.
     */
    @Transactional(readOnly = true)
    public Page<DocumentReponse> lister(String categorie, Pageable pageable) {
        Page<Document> page;
        if (categorie != null && !categorie.isBlank()) {
            page = documents.findByCategoryAndDeletedAtIsNull(
                    CategorieDocument.depuisCode(categorie), pageable);
        } else {
            page = documents.findByDeletedAtIsNull(pageable);
        }
        return page.map(d -> DocumentReponse.depuis(d, visibilites.rolesAutorises(d.getId())));
    }

    /**
     * Recherche par titre (documents actifs).
     */
    @Transactional(readOnly = true)
    public Page<DocumentReponse> rechercher(String terme, Pageable pageable) {
        return documents.rechercherParTitre(terme == null ? "" : terme, pageable)
                .map(d -> DocumentReponse.depuis(d, visibilites.rolesAutorises(d.getId())));
    }

    /**
     * Met à jour les métadonnées et la visibilité d'un document, puis émet un événement Updated.
     */
    @Transactional
    public DocumentReponse mettreAJour(UUID id, MettreAJourDocumentRequete requete, String traceId) {
        Document document = chargerActif(id);

        if (requete.title() != null && !requete.title().isBlank()) {
            document.setTitle(requete.title());
        }
        if (requete.description() != null) {
            document.setDescription(requete.description());
        }
        if (requete.archived() != null) {
            document.setArchived(requete.archived());
        }

        List<String> roles;
        if (requete.visibility() != null) {
            // Remplacement complet de la visibilité.
            visibilites.deleteByCleDocumentId(id);
            roles = enregistrerVisibilite(id, requete.visibility());
        } else {
            roles = visibilites.rolesAutorises(id);
        }

        documents.save(document);
        ecrireOutbox(document, roles, "Updated", traceId);

        audit.succes("MODIFICATION_DOCUMENT", "document", id.toString());
        return DocumentReponse.depuis(document, roles);
    }

    /**
     * Supprime logiquement un document (soft delete), retire le binaire MinIO et émet un Deleted.
     */
    @Transactional
    public void supprimer(UUID id, String traceId) {
        Document document = chargerActif(id);
        document.setDeletedAt(Instant.now());
        documents.save(document);

        // Tombstone logique sur le topic delete (politique "delete" pour document.documents).
        UUID acteur = utilisateurCourant();
        DocumentSupprimeEvenement payload = new DocumentSupprimeEvenement(id, Instant.now(), acteur);
        ecrireOutboxPayload(id, "Deleted", payload, traceId);

        // Binaire retiré best-effort (la métadonnée fait foi).
        stockage.supprimer(document.getBucket(), document.getObjectKey());

        audit.succes("SUPPRESSION_DOCUMENT", "document", id.toString());
    }

    /**
     * Délivre une URL présignée de téléchargement. L'autorisation OPA au niveau objet est
     * appliquée en amont (contrôleur) : ici on suppose l'accès déjà validé.
     */
    @Transactional(readOnly = true)
    public UrlTelechargementReponse urlTelechargement(UUID id) {
        Document document = chargerActif(id);
        String url = stockage.urlTelechargement(document.getBucket(), document.getObjectKey());
        audit.succes("TELECHARGEMENT_DOCUMENT", "document", id.toString());
        return new UrlTelechargementReponse(
                url,
                stockage.instantExpiration(),
                nomFichierSur(document.getObjectKey()),
                document.getMimeType());
    }

    /**
     * Partage nominatif d'un document avec un utilisateur (lecture, ou édition si demandé).
     */
    @Transactional
    public void partager(UUID id, PartageRequete requete) {
        // On s'assure que le document existe (sinon 404).
        chargerActif(id);
        partages.save(new DocumentShare(id, requete.userId(), requete.canEdit()));
        audit.succes("PARTAGE_DOCUMENT", "document", id.toString());
    }

    // ------------------------------------------------------------------
    // Méthodes internes
    // ------------------------------------------------------------------

    /** Charge un document actif ou lève une 404 (anti-énumération). */
    private Document chargerActif(UUID id) {
        return documents.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Document introuvable."));
    }

    /** Enregistre la liste de rôles visibles et renvoie la liste normalisée. */
    private List<String> enregistrerVisibilite(UUID documentId, List<String> roles) {
        List<String> normalises = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                if (role != null && !role.isBlank()) {
                    String r = role.trim().toLowerCase();
                    if (!normalises.contains(r)) {
                        normalises.add(r);
                        visibilites.save(new DocumentVisibility(documentId, r));
                    }
                }
            }
        }
        return normalises;
    }

    /** Choisit le bucket selon la catégorie (courrier -> bucket dédié). */
    private String bucketPour(CategorieDocument categorie) {
        return categorie == CategorieDocument.COURRIER
                ? minioProprietes.bucketCourriers()
                : minioProprietes.bucketDefaut();
    }

    /** Valide l'upload : présence, taille max et type MIME (liste blanche, OWASP). */
    private void validerFichier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est obligatoire.");
        }
        if (fichier.getSize() > uploadProprietes.tailleMaxOctets()) {
            throw new IllegalArgumentException("Fichier trop volumineux.");
        }
        if (!uploadProprietes.estMimeAutorise(fichier.getContentType())) {
            throw new IllegalArgumentException("Type de fichier non autorisé.");
        }
    }

    /** Écrit l'état complet de l'agrégat en Outbox (Created/Updated). */
    private void ecrireOutbox(Document document, List<String> roles, String eventType, String traceId) {
        DocumentEtatEvenement etat = DocumentEtatEvenement.depuis(document, roles);
        ecrireOutboxPayload(document.getId(), eventType, etat, traceId);
    }

    /** Sérialise un payload et l'écrit en Outbox sur le topic document.documents. */
    private void ecrireOutboxPayload(UUID aggregateId, String eventType, Object payload, String traceId) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outbox.save(new OutboxMessage(
                    EnteteEvenement.AGGREGAT_DOCUMENT,
                    aggregateId,
                    Topics.DOCUMENT_DOCUMENTS,
                    eventType,
                    VERSION_EVENEMENT,
                    json,
                    traceId));
        } catch (JsonProcessingException ex) {
            // Sérialisation impossible : on ne doit pas valider la transaction silencieusement.
            throw new IllegalStateException("Sérialisation de l'événement impossible.", ex);
        }
    }

    /** Calcule le SHA-256 du binaire pour le contrôle d'intégrité. */
    private String calculerChecksum(MultipartFile fichier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] empreinte = digest.digest(fichier.getBytes());
            return HexFormat.of().formatHex(empreinte);
        } catch (NoSuchAlgorithmException | IOException ex) {
            // Le checksum est facultatif : en cas d'échec, on n'interrompt pas le dépôt.
            log.warn("Calcul du checksum impossible : {}", ex.getMessage());
            return null;
        }
    }

    /** Identifiant de l'utilisateur courant (claim sub), ou erreur si non authentifié. */
    private UUID utilisateurCourant() {
        EntreeOpa.Sujet sujet = ContexteSecurite.sujetCourant();
        if (sujet.id() == null) {
            throw new IllegalStateException("Utilisateur non authentifié.");
        }
        return UUID.fromString(sujet.id());
    }

    /** Extrait un nom de fichier sûr (dernier segment du chemin, sans séparateur). */
    private String nomFichierSur(String nomOrigine) {
        if (nomOrigine == null || nomOrigine.isBlank()) {
            return "document";
        }
        String base = nomOrigine.replace('\\', '/');
        int dernier = base.lastIndexOf('/');
        String nom = dernier >= 0 ? base.substring(dernier + 1) : base;
        // On retire tout caractère problématique pour la clé S3 / l'en-tête de réponse.
        return nom.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
