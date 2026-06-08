package sn.unchk.office.insertion.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.common.authz.VerifieAccesObjet;
import sn.unchk.office.insertion.dto.PartnerRequest;
import sn.unchk.office.insertion.dto.PartnerResponse;
import sn.unchk.office.insertion.service.PartenaireService;

import java.util.List;
import java.util.UUID;

/**
 * API REST de la base de partenaires (structures d'accueil).
 * <p>
 * Chemins sous {@code /api/insertion/partenaires}. Le RBAC (rôle × route) est appliqué au
 * gateway via OPA (seuls admin et appui-insertion accèdent à {@code /api/insertion/**}).
 * La consultation d'un partenaire précis passe en plus par l'ABAC anti-IDOR
 * ({@link VerifieAccesObjet}).
 */
@RestController
@RequestMapping("/api/insertion/partenaires")
public class PartenaireController {

    private final PartenaireService service;

    public PartenaireController(PartenaireService service) {
        this.service = service;
    }

    /** Liste des partenaires actifs. */
    @GetMapping
    public List<PartnerResponse> lister() {
        return service.lister().stream().map(PartnerResponse::depuis).toList();
    }

    /** Consultation d'un partenaire (ABAC anti-IDOR au niveau objet). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "partenaire", action = "read", idParam = "id")
    public PartnerResponse consulter(@PathVariable UUID id) {
        return PartnerResponse.depuis(service.consulter(id));
    }

    /** Création d'un partenaire. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse creer(@Valid @RequestBody PartnerRequest requete) {
        return PartnerResponse.depuis(service.creer(requete));
    }

    /** Mise à jour d'un partenaire (ABAC anti-IDOR au niveau objet). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "partenaire", action = "update", idParam = "id")
    public PartnerResponse modifier(@PathVariable UUID id, @Valid @RequestBody PartnerRequest requete) {
        return PartnerResponse.depuis(service.modifier(id, requete));
    }

    /** Suppression logique d'un partenaire (ABAC anti-IDOR au niveau objet). */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = "partenaire", action = "delete", idParam = "id")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
