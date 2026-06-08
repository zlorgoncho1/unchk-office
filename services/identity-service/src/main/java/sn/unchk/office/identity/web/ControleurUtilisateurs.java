package sn.unchk.office.identity.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import sn.unchk.office.identity.dto.RequeteChangementMotDePasse;
import sn.unchk.office.identity.dto.RequeteCreationUtilisateur;
import sn.unchk.office.identity.dto.RequeteMajUtilisateur;
import sn.unchk.office.identity.dto.VueUtilisateur;
import sn.unchk.office.identity.service.ServiceUtilisateur;

import java.util.List;
import java.util.UUID;

/**
 * Gestion des comptes utilisateurs (CRUD), réservée à l'administrateur.
 * <p>
 * Préfixe {@code /api/identity/users} (routé par le gateway). Le RBAC grossier est déjà
 * appliqué au gateway via OPA ; ici on impose en plus le rôle {@code admin} au niveau méthode
 * (défense en profondeur) et l'ABAC objet anti-IDOR sur la consultation d'un compte précis
 * ({@link VerifieAccesObjet}).
 */
@RestController
@RequestMapping("/api/identity/users")
@PreAuthorize("hasRole('admin')")
public class ControleurUtilisateurs {

    private final ServiceUtilisateur serviceUtilisateur;

    public ControleurUtilisateurs(ServiceUtilisateur serviceUtilisateur) {
        this.serviceUtilisateur = serviceUtilisateur;
    }

    /** Liste tous les comptes (admin uniquement). */
    @GetMapping
    public List<VueUtilisateur> lister() {
        return serviceUtilisateur.lister();
    }

    /**
     * Consulte un compte précis. L'accès au niveau objet est vérifié par OPA (anti-IDOR) :
     * la garde charge le type {@code user} et l'identifiant pour la décision ABAC.
     */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "user", action = "read", idParam = "id")
    public VueUtilisateur consulter(@PathVariable UUID id) {
        return serviceUtilisateur.consulter(id);
    }

    /** Crée un compte et lui affecte ses rôles. Émet identity.users (Created). */
    @PostMapping
    public ResponseEntity<VueUtilisateur> creer(@Valid @RequestBody RequeteCreationUtilisateur requete,
                                                @AuthenticationPrincipal Jwt jwt) {
        VueUtilisateur vue = serviceUtilisateur.creer(requete, sujetCourant(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(vue);
    }

    /**
     * Met à jour un compte (nom, statut, rôles). ABAC objet vérifié par OPA. Émet identity.users (Updated).
     */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "user", action = "update", idParam = "id")
    public VueUtilisateur mettreAJour(@PathVariable UUID id,
                                      @Valid @RequestBody RequeteMajUtilisateur requete,
                                      @AuthenticationPrincipal Jwt jwt) {
        return serviceUtilisateur.mettreAJour(id, requete, sujetCourant(jwt));
    }

    /** Réinitialise le mot de passe d'un compte. ABAC objet vérifié par OPA. */
    @PutMapping("/{id}/password")
    @VerifieAccesObjet(type = "user", action = "update", idParam = "id")
    public ResponseEntity<Void> changerMotDePasse(@PathVariable UUID id,
                                                  @Valid @RequestBody RequeteChangementMotDePasse requete) {
        serviceUtilisateur.changerMotDePasse(id, requete.nouveauMotDePasse());
        return ResponseEntity.noContent().build();
    }

    /** Supprime (logiquement) un compte. ABAC objet vérifié par OPA. Émet identity.users (Deleted). */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = "user", action = "delete", idParam = "id")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        serviceUtilisateur.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    /** Extrait l'UUID de l'admin courant depuis le jeton (pour l'auditer comme auteur). */
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
