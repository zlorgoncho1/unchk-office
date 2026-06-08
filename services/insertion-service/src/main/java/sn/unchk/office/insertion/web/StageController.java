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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.common.authz.VerifieAccesObjet;
import sn.unchk.office.insertion.dto.InternshipRequest;
import sn.unchk.office.insertion.dto.InternshipResponse;
import sn.unchk.office.insertion.service.StageService;

import java.util.List;
import java.util.UUID;

/**
 * API REST des stages (bilans de stages).
 * <p>
 * Chemins sous {@code /api/insertion/stages}. La consultation d'un stage précis est protégée
 * par l'ABAC anti-IDOR : un étudiant ne consulte QUE son propre bilan (ownerId = studentRef).
 */
@RestController
@RequestMapping("/api/insertion/stages")
public class StageController {

    private final StageService service;

    public StageController(StageService service) {
        this.service = service;
    }

    /**
     * Liste des stages, éventuellement filtrée par étudiant.
     *
     * @param etudiant identifiant d'étudiant (optionnel)
     */
    @GetMapping
    public List<InternshipResponse> lister(@RequestParam(name = "etudiant", required = false) UUID etudiant) {
        List<sn.unchk.office.insertion.domain.Internship> stages =
                etudiant != null ? service.listerParEtudiant(etudiant) : service.lister();
        return stages.stream().map(InternshipResponse::depuis).toList();
    }

    /** Consultation d'un stage (ABAC anti-IDOR au niveau objet). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "stage", action = "read", idParam = "id")
    public InternshipResponse consulter(@PathVariable UUID id) {
        return InternshipResponse.depuis(service.consulter(id));
    }

    /** Création d'un stage. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InternshipResponse creer(@Valid @RequestBody InternshipRequest requete) {
        return InternshipResponse.depuis(service.creer(requete));
    }

    /** Mise à jour / clôture d'un stage (ABAC anti-IDOR au niveau objet). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "stage", action = "update", idParam = "id")
    public InternshipResponse modifier(@PathVariable UUID id, @Valid @RequestBody InternshipRequest requete) {
        return InternshipResponse.depuis(service.modifier(id, requete));
    }

    /** Suppression logique d'un stage (ABAC anti-IDOR au niveau objet). */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = "stage", action = "delete", idParam = "id")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
