package sn.unchk.office.identity.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.identity.dto.ReponseJetons;
import sn.unchk.office.identity.dto.RequeteConnexion;
import sn.unchk.office.identity.dto.RequeteRafraichissement;
import sn.unchk.office.identity.securite.ServiceCleSignature;
import sn.unchk.office.identity.service.ServiceAuthentification;

import java.util.Map;

/**
 * Endpoints publics d'authentification et d'exposition des clés (JWKS).
 * <p>
 * Préfixe {@code /api/identity/auth} (routé par le gateway via {@code /api/identity/**}).
 * Ces endpoints sont en liste blanche au gateway (pas de JWT requis) et publics côté service
 * (cf. {@code ConfigurationSecuriteIdentity}).
 */
@RestController
@RequestMapping("/api/identity/auth")
public class ControleurAuth {

    private final ServiceAuthentification serviceAuth;
    private final ServiceCleSignature serviceCle;

    public ControleurAuth(ServiceAuthentification serviceAuth, ServiceCleSignature serviceCle) {
        this.serviceAuth = serviceAuth;
        this.serviceCle = serviceCle;
    }

    /** Connexion : vérifie les identifiants et délivre access + refresh tokens. */
    @PostMapping("/login")
    public ResponseEntity<ReponseJetons> connexion(@Valid @RequestBody RequeteConnexion requete,
                                                   HttpServletRequest http) {
        ReponseJetons jetons = serviceAuth.connexion(
                requete.email(),
                requete.motDePasse(),
                RequeteHttpUtil.adresseIp(http),
                RequeteHttpUtil.userAgent(http));
        return ResponseEntity.ok(jetons);
    }

    /** Rafraîchissement : échange un refresh token valide contre de nouveaux jetons. */
    @PostMapping("/refresh")
    public ResponseEntity<ReponseJetons> rafraichir(@Valid @RequestBody RequeteRafraichissement requete,
                                                    HttpServletRequest http) {
        ReponseJetons jetons = serviceAuth.rafraichir(
                requete.refreshToken(),
                RequeteHttpUtil.adresseIp(http),
                RequeteHttpUtil.userAgent(http));
        return ResponseEntity.ok(jetons);
    }

    /** Déconnexion : révoque les refresh tokens de l'utilisateur. */
    @PostMapping("/logout")
    public ResponseEntity<Void> deconnexion(@Valid @RequestBody RequeteRafraichissement requete,
                                            HttpServletRequest http) {
        serviceAuth.deconnexion(
                requete.refreshToken(),
                RequeteHttpUtil.adresseIp(http),
                RequeteHttpUtil.userAgent(http));
        return ResponseEntity.noContent().build();
    }

    /**
     * JWKS exposé sous {@code /api/identity/auth/.well-known/jwks.json} (accès via le gateway).
     * Ne contient que les clés PUBLIQUES.
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return serviceCle.jwks();
    }
}
