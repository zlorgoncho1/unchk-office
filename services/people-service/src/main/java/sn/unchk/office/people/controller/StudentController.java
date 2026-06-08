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
import sn.unchk.office.common.authz.VerifieAccesObjet;
import sn.unchk.office.people.dto.CreerEtudiantRequest;
import sn.unchk.office.people.dto.EtudiantResponse;
import sn.unchk.office.people.dto.ModifierEtudiantRequest;
import sn.unchk.office.people.service.StudentService;

import java.net.URI;
import java.util.UUID;

/**
 * API REST des etudiants canoniques, exposee sous {@code /api/people/students}
 * (route du gateway {@code /api/people/**}).
 * <p>
 * Le gateway applique le RBAC (role x route) en amont. Ici on ajoute l'ABAC objet
 * (anti-IDOR) sur la consultation d'une fiche precise via {@link VerifieAccesObjet} :
 * OPA decide selon le proprietaire et la visibilite reels charges en base.
 */
@RestController
@RequestMapping("/api/people/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    /** Liste paginee des etudiants actifs (reservee au personnel par le RBAC du gateway). */
    @GetMapping
    public Page<EtudiantResponse> lister(Pageable pageable) {
        return service.lister(pageable);
    }

    /**
     * Consulte la fiche d'un etudiant par UUID.
     * <p>
     * Protege par OPA au niveau objet : si l'acces est refuse, le service renvoie 404
     * (anti-enumeration) plutot que 403, pour ne pas confirmer l'existence de l'UUID.
     */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "etudiant", action = "read", idParam = "id")
    public EtudiantResponse consulter(@PathVariable UUID id) {
        return service.consulter(id);
    }

    /** Cree un etudiant et renvoie 201 avec l'en-tete Location. */
    @PostMapping
    public ResponseEntity<EtudiantResponse> creer(@Valid @RequestBody CreerEtudiantRequest requete,
                                                  @AuthenticationPrincipal Jwt jwt) {
        EtudiantResponse cree = service.creer(requete, sujetCourant(jwt));
        return ResponseEntity
                .created(URI.create("/api/people/students/" + cree.id()))
                .body(cree);
    }

    /** Met a jour un etudiant existant (acces objet verifie par OPA avant ecriture). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "etudiant", action = "update", idParam = "id")
    public EtudiantResponse modifier(@PathVariable UUID id,
                                     @Valid @RequestBody ModifierEtudiantRequest requete) {
        return service.modifier(id, requete);
    }

    /** Suppression logique d'un etudiant (acces objet verifie par OPA), renvoie 204. */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = "etudiant", action = "delete", idParam = "id")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id,
                                          @AuthenticationPrincipal Jwt jwt) {
        service.supprimer(id, sujetCourant(jwt));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /** Extrait l'identifiant du sujet (claim {@code sub}) du jeton, ou null hors contexte. */
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
