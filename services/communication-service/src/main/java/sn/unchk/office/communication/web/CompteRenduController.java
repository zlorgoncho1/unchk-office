package sn.unchk.office.communication.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.common.authz.VerifieAccesObjet;
import sn.unchk.office.communication.dto.CompteRenduCreationRequest;
import sn.unchk.office.communication.dto.CompteRenduDto;
import sn.unchk.office.communication.security.FournisseurAttributsCommunication;
import sn.unchk.office.communication.service.ServiceCompteRendu;

import java.util.List;
import java.util.UUID;

/**
 * API REST des comptes rendus, sous {@code /api/communication/comptes-rendus}.
 * <p>
 * La consultation d'un compte rendu par identifiant est protégée par l'ABAC objet (anti-IDOR) :
 * l'annotation {@link VerifieAccesObjet} déclenche OPA avec le {@code ownerId} et la
 * {@code visibility} réels chargés en base (cf. {@link FournisseurAttributsCommunication}).
 * Sur refus en lecture, la garde se traduit en 403/404 sobre (pas de fuite d'existence).
 */
@RestController
@RequestMapping("/api/communication/comptes-rendus")
public class CompteRenduController {

    private final ServiceCompteRendu serviceCompteRendu;

    public CompteRenduController(ServiceCompteRendu serviceCompteRendu) {
        this.serviceCompteRendu = serviceCompteRendu;
    }

    /** Rédige un compte rendu (brouillon, non publié). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompteRenduDto rediger(@Valid @RequestBody CompteRenduCreationRequest requete) {
        return serviceCompteRendu.rediger(requete, UtilisateurCourant.id());
    }

    /** Publie un compte rendu : déclenche les notifications. Réservé au propriétaire (ABAC update). */
    @PatchMapping("/{id}/publish")
    @VerifieAccesObjet(type = FournisseurAttributsCommunication.TYPE_COMPTE_RENDU,
            action = "update", idParam = "id")
    public CompteRenduDto publier(@PathVariable UUID id) {
        return serviceCompteRendu.publier(id);
    }

    /** Liste les comptes rendus (les plus récents d'abord). */
    @GetMapping
    public List<CompteRenduDto> lister() {
        return serviceCompteRendu.lister();
    }

    /** Consulte un compte rendu par identifiant — ABAC objet (visibilité par rôle / propriétaire). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = FournisseurAttributsCommunication.TYPE_COMPTE_RENDU,
            action = "read", idParam = "id")
    public CompteRenduDto consulter(@PathVariable UUID id) {
        return serviceCompteRendu.consulter(id);
    }

    /** Modifie un compte rendu (corps = même DTO que la rédaction). Réservé au propriétaire (ABAC update). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = FournisseurAttributsCommunication.TYPE_COMPTE_RENDU,
            action = "update", idParam = "id")
    public CompteRenduDto modifier(@PathVariable UUID id,
                                   @Valid @RequestBody CompteRenduCreationRequest requete) {
        return serviceCompteRendu.modifier(id, requete);
    }

    /** Supprime un compte rendu (suppression logique). Réservé au propriétaire (ABAC delete). */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = FournisseurAttributsCommunication.TYPE_COMPTE_RENDU,
            action = "delete", idParam = "id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID id) {
        serviceCompteRendu.supprimer(id);
    }
}
