package sn.unchk.office.academic.formation;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import sn.unchk.office.academic.formation.dto.FormationCreationDto;
import sn.unchk.office.academic.formation.dto.FormationDto;
import sn.unchk.office.academic.formation.dto.FormationMajDto;
import sn.unchk.office.common.authz.ContexteSecurite;
import sn.unchk.office.common.authz.VerifieAccesObjet;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * API REST des formations, exposée sous {@code /api/academic/formations}.
 * <p>
 * Le RBAC grossier (rôle × route) est appliqué en amont par le gateway via OPA. Ici, les
 * accès à un objet identifié par UUID sont protégés par l'ABAC anti-IDOR
 * ({@link VerifieAccesObjet}) : le {@code ResourceAccessGuard} interroge OPA avec les attributs
 * de la formation (propriétaire, visibilité) fournis par {@code FournisseurAttributsFormation}.
 */
@RestController
@RequestMapping("/api/academic/formations")
public class FormationController {

    private final FormationService formationService;

    public FormationController(FormationService formationService) {
        this.formationService = formationService;
    }

    /**
     * Liste les formations (optionnellement filtrées par niveau).
     * Endpoint de collection : le RBAC de route au gateway suffit (pas d'objet ciblé).
     */
    @GetMapping
    public List<FormationDto> lister(@RequestParam(value = "niveau", required = false) String niveau) {
        // On résout le libellé de niveau (ex : « licence ») de façon tolérante à la casse.
        NiveauFormation niveauResolu = (niveau != null && !niveau.isBlank())
                ? NiveauFormation.depuis(niveau)
                : null;
        List<Formation> formations = (niveauResolu != null)
                ? formationService.listerParNiveau(niveauResolu)
                : formationService.lister();
        return formations.stream().map(FormationDto::de).toList();
    }

    /**
     * Consulte une formation par son identifiant.
     * <p>
     * Endpoint sensible (accès objet par UUID) : protégé par l'ABAC anti-IDOR. En cas de refus
     * en lecture, la garde lève une erreur générique (anti-énumération).
     */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "formation", action = "read", idParam = "id")
    public FormationDto consulter(@PathVariable UUID id) {
        return FormationDto.de(formationService.obtenir(id));
    }

    /**
     * Crée une formation. Le créateur est déduit du jeton (claim {@code sub}).
     */
    @PostMapping
    public ResponseEntity<FormationDto> creer(@Valid @RequestBody FormationCreationDto dto,
                                              UriComponentsBuilder uriBuilder) {
        UUID createur = sujetCourant();
        Formation creee = formationService.creer(dto, createur);
        URI emplacement = uriBuilder.path("/api/academic/formations/{id}")
                .buildAndExpand(creee.getId()).toUri();
        return ResponseEntity.created(emplacement).body(FormationDto.de(creee));
    }

    /**
     * Met à jour une formation (endpoint sensible : ABAC anti-IDOR en écriture).
     */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "formation", action = "update", idParam = "id")
    public FormationDto mettreAJour(@PathVariable UUID id,
                                    @Valid @RequestBody FormationMajDto dto) {
        return FormationDto.de(formationService.mettreAJour(id, dto));
    }

    /**
     * Supprime logiquement une formation (endpoint sensible : ABAC anti-IDOR en suppression).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @VerifieAccesObjet(type = "formation", action = "delete", idParam = "id")
    public void supprimer(@PathVariable UUID id) {
        formationService.supprimer(id, sujetCourant());
    }

    /** Identifiant de l'utilisateur courant (claim {@code sub}), pour l'audit et l'auteur. */
    private UUID sujetCourant() {
        String id = ContexteSecurite.sujetCourant().id();
        return id != null ? UUID.fromString(id) : null;
    }
}
