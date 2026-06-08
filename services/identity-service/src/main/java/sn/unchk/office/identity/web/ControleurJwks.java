package sn.unchk.office.identity.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.identity.securite.ServiceCleSignature;

import java.util.Map;

/**
 * Exposition du JWKS à la racine ({@code /.well-known/jwks.json}).
 * <p>
 * C'est l'emplacement standard interrogé directement par le gateway et les services métier
 * (sur le réseau Docker) pour valider la signature RS256 des jetons. Ne contient que les
 * clés PUBLIQUES ; la clé privée n'est jamais exposée.
 */
@RestController
public class ControleurJwks {

    private final ServiceCleSignature serviceCle;

    public ControleurJwks(ServiceCleSignature serviceCle) {
        this.serviceCle = serviceCle;
    }

    /** Renvoie le document JWKS (clés publiques de signature). */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return serviceCle.jwks();
    }
}
