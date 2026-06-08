package sn.unchk.office.people.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.people.dto.CreerPersonnelRequest;
import sn.unchk.office.people.dto.ModifierPersonnelRequest;
import sn.unchk.office.people.dto.PersonnelResponse;
import sn.unchk.office.people.service.StaffService;

import java.net.URI;
import java.util.UUID;

/**
 * API REST du personnel / formateurs canoniques, exposee sous {@code /api/people/staff}
 * (route du gateway {@code /api/people/**}).
 * <p>
 * Le RBAC (role x route) est applique par le gateway. Le personnel n'est pas une donnee
 * "propre a un utilisateur" (pas d'ABAC proprietaire ici, contrairement a la fiche etudiant) ;
 * la consultation reste neanmoins reservee aux roles autorises par le RBAC.
 */
@RestController
@RequestMapping("/api/people/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    /** Liste paginee du personnel actif. */
    @GetMapping
    public Page<PersonnelResponse> lister(Pageable pageable) {
        return service.lister(pageable);
    }

    /** Consulte un membre du personnel par UUID ; 404 s'il est inconnu. */
    @GetMapping("/{id}")
    public PersonnelResponse consulter(@PathVariable UUID id) {
        return service.consulter(id);
    }

    /** Cree un membre du personnel et renvoie 201 avec l'en-tete Location. */
    @PostMapping
    public ResponseEntity<PersonnelResponse> creer(@Valid @RequestBody CreerPersonnelRequest requete,
                                                   @AuthenticationPrincipal Jwt jwt) {
        PersonnelResponse cree = service.creer(requete, sujetCourant(jwt));
        return ResponseEntity
                .created(URI.create("/api/people/staff/" + cree.id()))
                .body(cree);
    }

    /** Met a jour un membre du personnel existant. */
    @PutMapping("/{id}")
    public PersonnelResponse modifier(@PathVariable UUID id,
                                      @Valid @RequestBody ModifierPersonnelRequest requete) {
        return service.modifier(id, requete);
    }

    /** Suppression logique d'un membre du personnel, renvoie 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id,
                                          @AuthenticationPrincipal Jwt jwt) {
        service.supprimer(id, sujetCourant(jwt));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private UUID sujetCourant(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
