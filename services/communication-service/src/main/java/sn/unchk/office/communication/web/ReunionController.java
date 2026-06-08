package sn.unchk.office.communication.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.communication.dto.ReunionCreationRequest;
import sn.unchk.office.communication.dto.ReunionDto;
import sn.unchk.office.communication.service.ServiceReunion;

import java.util.List;
import java.util.UUID;

/**
 * API REST des réunions, sous {@code /api/communication/reunions}.
 * <p>
 * Le RBAC (rôle × route) est appliqué au gateway. L'identité de l'auteur est résolue
 * côté serveur (jamais depuis le corps de requête) pour fixer le propriétaire.
 */
@RestController
@RequestMapping("/api/communication/reunions")
public class ReunionController {

    private final ServiceReunion serviceReunion;

    public ReunionController(ServiceReunion serviceReunion) {
        this.serviceReunion = serviceReunion;
    }

    /** Planifie une réunion et invite ses participants. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReunionDto planifier(@Valid @RequestBody ReunionCreationRequest requete) {
        // Le créateur (propriétaire) est l'utilisateur authentifié, pas une valeur du client.
        return serviceReunion.planifier(requete, UtilisateurCourant.id());
    }

    /** Liste les réunions (les plus récentes d'abord). */
    @GetMapping
    public List<ReunionDto> lister() {
        return serviceReunion.lister();
    }

    /** Consulte une réunion par identifiant. */
    @GetMapping("/{id}")
    public ResponseEntity<ReunionDto> consulter(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceReunion.consulter(id));
    }
}
