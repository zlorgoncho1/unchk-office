package sn.unchk.office.insertion.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.common.authz.VerifieAccesObjet;
import sn.unchk.office.insertion.dto.InsertionOutcomeRequest;
import sn.unchk.office.insertion.dto.InsertionOutcomeResponse;
import sn.unchk.office.insertion.service.InsertionOutcomeService;

import java.util.List;
import java.util.UUID;

/**
 * API REST des situations d'insertion (suivi du devenir et support des statistiques).
 * <p>
 * Chemins sous {@code /api/insertion/situations}. La consultation par étudiant est protégée
 * par l'ABAC anti-IDOR (un étudiant ne consulte que sa propre situation).
 */
@RestController
@RequestMapping("/api/insertion/situations")
public class InsertionOutcomeController {

    private final InsertionOutcomeService service;

    public InsertionOutcomeController(InsertionOutcomeService service) {
        this.service = service;
    }

    /** Situations d'insertion d'un étudiant (ABAC anti-IDOR : ownerId = studentRef). */
    @GetMapping("/etudiant/{studentRef}")
    @VerifieAccesObjet(type = "insertion", action = "read", idParam = "studentRef")
    public List<InsertionOutcomeResponse> parEtudiant(@PathVariable UUID studentRef) {
        return service.listerParEtudiant(studentRef).stream()
                .map(InsertionOutcomeResponse::depuis).toList();
    }

    /** Consultation d'une situation précise (ABAC anti-IDOR au niveau objet). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "insertion", action = "read", idParam = "id")
    public InsertionOutcomeResponse consulter(@PathVariable UUID id) {
        return InsertionOutcomeResponse.depuis(service.consulter(id));
    }

    /** Déclaration d'une situation d'insertion. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsertionOutcomeResponse declarer(@Valid @RequestBody InsertionOutcomeRequest requete) {
        return InsertionOutcomeResponse.depuis(service.declarer(requete));
    }

    /** Mise à jour d'une situation (ABAC anti-IDOR au niveau objet). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "insertion", action = "update", idParam = "id")
    public InsertionOutcomeResponse modifier(@PathVariable UUID id,
                                             @Valid @RequestBody InsertionOutcomeRequest requete) {
        return InsertionOutcomeResponse.depuis(service.modifier(id, requete));
    }
}
