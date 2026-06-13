package sn.unchk.office.communication.web;

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
import sn.unchk.office.communication.dto.ReunionCreationRequest;
import sn.unchk.office.communication.dto.ReunionDto;
import sn.unchk.office.communication.security.FournisseurAttributsCommunication;
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

    /** Consulte une réunion par identifiant — ABAC objet (créateur ou admin). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = FournisseurAttributsCommunication.TYPE_REUNION,
            action = "read", idParam = "id")
    public ResponseEntity<ReunionDto> consulter(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceReunion.consulter(id));
    }

    /** Modifie une réunion (corps = même DTO que la création). Réservé au propriétaire (ABAC update). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = FournisseurAttributsCommunication.TYPE_REUNION,
            action = "update", idParam = "id")
    public ReunionDto modifier(@PathVariable UUID id,
                               @Valid @RequestBody ReunionCreationRequest requete) {
        return serviceReunion.modifier(id, requete);
    }

    /** Supprime une réunion (suppression logique). Réservé au propriétaire (ABAC delete). */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = FournisseurAttributsCommunication.TYPE_REUNION,
            action = "delete", idParam = "id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        serviceReunion.supprimer(id);
    }
}
