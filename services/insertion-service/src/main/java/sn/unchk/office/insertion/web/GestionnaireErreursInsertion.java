package sn.unchk.office.insertion.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestion locale des erreurs propres au service d'insertion, en complément du
 * gestionnaire global fourni par la librairie commune.
 * <p>
 * Annoté avec une priorité haute ({@link Order}) pour que le 404 « ressource introuvable »
 * soit pris en charge ici plutôt que par le filet de sécurité générique (qui renverrait 500).
 */
@RestControllerAdvice
@Order(0)
public class GestionnaireErreursInsertion {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreursInsertion.class);

    /**
     * Ressource introuvable : 404 neutre (ne révèle rien sur l'existence réelle de l'UUID).
     */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ProblemDetail> gererIntrouvable(RessourceIntrouvableException ex,
                                                          HttpServletRequest requete) {
        log.debug("Ressource introuvable sur {} : {}", requete.getRequestURI(), ex.getMessage());
        ProblemDetail corps = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Ressource introuvable.");
        corps.setInstance(java.net.URI.create(requete.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corps);
    }
}
