package sn.unchk.office.document.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sn.unchk.office.common.authz.VerifieAccesObjet;
import sn.unchk.office.common.web.CorrelationIdFilter;
import sn.unchk.office.document.dto.CreerDocumentRequete;
import sn.unchk.office.document.dto.DocumentReponse;
import sn.unchk.office.document.dto.MettreAJourDocumentRequete;
import sn.unchk.office.document.dto.PartageRequete;
import sn.unchk.office.document.dto.UrlTelechargementReponse;
import sn.unchk.office.document.service.DocumentService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * API REST de gestion documentaire, exposée sous {@code /api/documents}.
 * <p>
 * Les endpoints sensibles (consultation, téléchargement, modification, suppression d'un
 * document désigné par son UUID) sont protégés par {@link VerifieAccesObjet} : la garde OPA
 * vérifie l'accès au niveau objet (anti-IDOR), en s'appuyant sur la visibilité par rôle et
 * le propriétaire chargés depuis la base locale ({@code FournisseurAttributsDocument}).
 * <p>
 * Le RBAC grossier (rôle × route) est appliqué en amont par le gateway via OPA.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    /**
     * Dépose un nouveau document (multipart : métadonnées + fichier binaire).
     */
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentReponse creer(@RequestPart("metadata") @Valid CreerDocumentRequete metadata,
                                 @RequestPart("file") MultipartFile file,
                                 HttpServletRequest requete) {
        return service.creer(metadata, file, correlationId(requete));
    }

    /**
     * Liste paginée des documents, filtrable par catégorie ({@code ?category=circulaire}).
     */
    @GetMapping
    public Page<DocumentReponse> lister(@RequestParam(name = "category", required = false) String category,
                                        @PageableDefault(size = 20) Pageable pageable) {
        return service.lister(category, pageable);
    }

    /**
     * Recherche par titre ({@code ?q=...}).
     */
    @GetMapping("/recherche")
    public Page<DocumentReponse> rechercher(@RequestParam(name = "q", required = false) String q,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return service.rechercher(q, pageable);
    }

    /**
     * Consulte les métadonnées d'un document — contrôle OPA au niveau objet (anti-IDOR).
     */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "document", action = "read", idParam = "id")
    public DocumentReponse consulter(@PathVariable("id") UUID id) {
        return service.consulter(id);
    }

    /**
     * Délivre une URL présignée de téléchargement — contrôle OPA au niveau objet (anti-IDOR).
     */
    @GetMapping("/{id}/telechargement")
    @VerifieAccesObjet(type = "document", action = "read", idParam = "id")
    public UrlTelechargementReponse telecharger(@PathVariable("id") UUID id) {
        return service.urlTelechargement(id);
    }

    /**
     * Met à jour les métadonnées / la visibilité — contrôle OPA au niveau objet (anti-IDOR).
     */
    @PatchMapping("/{id}")
    @VerifieAccesObjet(type = "document", action = "update", idParam = "id")
    public DocumentReponse mettreAJour(@PathVariable("id") UUID id,
                                       @RequestBody @Valid MettreAJourDocumentRequete requete,
                                       HttpServletRequest httpRequete) {
        return service.mettreAJour(id, requete, correlationId(httpRequete));
    }

    /**
     * Partage nominatif d'un document avec un utilisateur — contrôle OPA (action update).
     */
    @PostMapping("/{id}/partages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @VerifieAccesObjet(type = "document", action = "update", idParam = "id")
    public void partager(@PathVariable("id") UUID id,
                         @RequestBody @Valid PartageRequete requete) {
        service.partager(id, requete);
    }

    /**
     * Supprime logiquement un document — contrôle OPA au niveau objet (anti-IDOR).
     */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = "document", action = "delete", idParam = "id")
    public ResponseEntity<Void> supprimer(@PathVariable("id") UUID id, HttpServletRequest requete) {
        service.supprimer(id, correlationId(requete));
        return ResponseEntity.noContent().build();
    }

    /** Récupère l'identifiant de corrélation posé par le filtre commun. */
    private String correlationId(HttpServletRequest requete) {
        Object valeur = requete.getAttribute(CorrelationIdFilter.ATTRIBUT_REQUETE);
        return valeur != null ? valeur.toString() : null;
    }
}
