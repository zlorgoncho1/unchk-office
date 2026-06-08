package sn.unchk.office.communication.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sn.unchk.office.communication.service.RessourceIntrouvableException;

/**
 * Gestion d'erreurs propre au service, en complément du gestionnaire global de {@code libs/common}.
 * <p>
 * Traduit l'absence de ressource en 404 sobre (RFC 7807, sans fuite d'interne). Cohérent avec
 * l'anti-énumération : un refus en lecture et une ressource inexistante sont indistincts (404).
 * Annoté avec une priorité haute pour primer sur le filet générique du gestionnaire commun.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GestionnaireErreursCommunication {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreursCommunication.class);

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail gererIntrouvable(RessourceIntrouvableException ex) {
        log.debug("Ressource introuvable : {}", ex.getMessage());
        ProblemDetail probleme = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        probleme.setTitle("Ressource introuvable");
        // Message générique : ne révèle pas l'existence ou non d'un UUID précis.
        probleme.setDetail("La ressource demandée est introuvable.");
        return probleme;
    }
}
