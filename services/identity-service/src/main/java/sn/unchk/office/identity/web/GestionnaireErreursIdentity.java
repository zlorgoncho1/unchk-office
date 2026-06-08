package sn.unchk.office.identity.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sn.unchk.office.common.web.ApiError;
import sn.unchk.office.common.web.CorrelationIdFilter;
import sn.unchk.office.identity.service.AuthentificationException;
import sn.unchk.office.identity.service.ConflitException;
import sn.unchk.office.identity.service.RessourceIntrouvableException;

/**
 * Gestion des exceptions propres à l'identity-service (auth, conflit, introuvable).
 * <p>
 * Priorité plus haute que le gestionnaire global de {@code common} : ces handlers
 * spécifiques s'appliquent avant le filet de sécurité générique. Les réponses restent
 * sobres (pas de fuite : on ne précise pas si c'est l'email ou le mot de passe qui est faux).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GestionnaireErreursIdentity {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreursIdentity.class);

    /** Identifiants invalides / compte inutilisable / refresh invalide : 401 générique. */
    @ExceptionHandler(AuthentificationException.class)
    public ResponseEntity<ApiError> gererAuth(AuthentificationException ex, HttpServletRequest requete) {
        // On journalise le motif réel côté serveur, mais on renvoie un message neutre au client.
        log.info("Échec d'authentification sur {} : {}", requete.getRequestURI(), ex.getMessage());
        return reponse(HttpStatus.UNAUTHORIZED, "Identifiants invalides.", requete);
    }

    /** Compte introuvable : 404. */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiError> gererIntrouvable(RessourceIntrouvableException ex,
                                                     HttpServletRequest requete) {
        return reponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", requete);
    }

    /** Conflit (ex : courriel déjà pris) : 409. */
    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ApiError> gererConflit(ConflitException ex, HttpServletRequest requete) {
        return reponse(HttpStatus.CONFLICT, ex.getMessage(), requete);
    }

    private ResponseEntity<ApiError> reponse(HttpStatus statut, String message, HttpServletRequest requete) {
        ApiError corps = ApiError.de(
                statut.value(),
                statut.getReasonPhrase(),
                message,
                requete.getRequestURI(),
                correlationId(requete));
        return ResponseEntity.status(statut).body(corps);
    }

    private String correlationId(HttpServletRequest requete) {
        Object valeur = requete.getAttribute(CorrelationIdFilter.ATTRIBUT_REQUETE);
        return valeur != null ? valeur.toString() : null;
    }
}
