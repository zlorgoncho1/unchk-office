package sn.unchk.office.admin.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.admin.dto.BudgetDto;
import sn.unchk.office.admin.dto.BudgetResumeDto;
import sn.unchk.office.admin.dto.ChangementStatutBudgetDto;
import sn.unchk.office.admin.dto.CreationBudgetDto;
import sn.unchk.office.admin.dto.CreationLigneBudgetaireDto;
import sn.unchk.office.admin.dto.MajBudgetDto;
import sn.unchk.office.admin.dto.RealisationLigneDto;
import sn.unchk.office.admin.service.BudgetService;
import sn.unchk.office.common.authz.VerifieAccesObjet;

import java.util.List;
import java.util.UUID;

/**
 * API REST de gestion budgétaire (sous {@code /api/admin/budgets}).
 * <p>
 * Le RBAC grossier (rôle × route) est appliqué au gateway. Sur les accès à un budget identifié
 * par UUID, l'annotation {@link VerifieAccesObjet} déclenche l'ABAC anti-IDOR (OPA) : la garde
 * charge le propriétaire/visibilité réels (via le fournisseur d'attributs) et OPA tranche.
 * Les corps de requête sont validés par Bean Validation ({@code @Valid}).
 */
@RestController
@RequestMapping("/api/admin/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /** Crée un projet de budget. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetDto creer(@Valid @RequestBody CreationBudgetDto dto) {
        return budgetService.creer(dto);
    }

    /** Liste les budgets, éventuellement filtrés par exercice ({@code ?annee=2026}). */
    @GetMapping
    public List<BudgetResumeDto> lister(@RequestParam(name = "annee", required = false) Short annee) {
        return budgetService.lister(annee);
    }

    /** Consulte un budget précis (anti-IDOR : ABAC OPA sur l'objet). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "budget", action = "read", idParam = "id")
    public BudgetDto consulter(@PathVariable UUID id) {
        return budgetService.consulter(id);
    }

    /** Met à jour les attributs d'un budget (propriétaire requis : ABAC update). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "budget", action = "update", idParam = "id")
    public BudgetDto mettreAJour(@PathVariable UUID id, @Valid @RequestBody MajBudgetDto dto) {
        return budgetService.mettreAJour(id, dto);
    }

    /** Fait évoluer le statut d'un budget. */
    @PatchMapping("/{id}/statut")
    @VerifieAccesObjet(type = "budget", action = "update", idParam = "id")
    public BudgetDto changerStatut(@PathVariable UUID id,
                                   @Valid @RequestBody ChangementStatutBudgetDto dto) {
        return budgetService.changerStatut(id, dto);
    }

    /** Ajoute une ligne budgétaire (poste prévu). */
    @PostMapping("/{id}/lignes")
    @ResponseStatus(HttpStatus.CREATED)
    @VerifieAccesObjet(type = "budget", action = "update", idParam = "id")
    public BudgetDto ajouterLigne(@PathVariable UUID id,
                                  @Valid @RequestBody CreationLigneBudgetaireDto dto) {
        return budgetService.ajouterLigne(id, dto);
    }

    /** Saisit le montant réalisé d'une ligne (budget réalisé). */
    @PatchMapping("/{id}/lignes/{ligneId}/realisation")
    @VerifieAccesObjet(type = "budget", action = "update", idParam = "id")
    public BudgetDto renseignerRealisation(@PathVariable UUID id,
                                           @PathVariable UUID ligneId,
                                           @Valid @RequestBody RealisationLigneDto dto) {
        return budgetService.renseignerRealisation(id, ligneId, dto);
    }
}
