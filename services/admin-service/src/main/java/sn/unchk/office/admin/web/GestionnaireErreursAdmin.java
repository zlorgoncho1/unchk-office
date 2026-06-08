package sn.unchk.office.admin.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sn.unchk.office.admin.service.ConflitRessourceException;
import sn.unchk.office.admin.service.RessourceIntrouvableException;
import sn.unchk.office.common.web.ApiError;
import sn.unchk.office.common.web.CorrelationIdFilter;

/**
 * Gestionnaire d'erreurs propre au service Administration.
 * <p>
 * Complète le {@code GlobalExceptionHandler} de libs/common pour les exceptions métier locales
 * (ressource introuvable → 404 ; conflit d'unicité → 409). Déclaré prioritaire pour être pris
 * en compte avant le filet générique de la librairie. Réponses sobres (RFC 7807-like) sans fuite.
 */
@RestControllerAdvice
@Order(0)
public class GestionnaireErreursAdmin {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreursAdmin.class);

    /** Ressource inexistante ou non visible : 404 (anti-énumération). */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiError> gererIntrouvable(RessourceIntrouvableException ex,
                                                     HttpServletRequest requete) {
        log.info("Ressource introuvable sur {}", requete.getRequestURI());
        return reponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", requete);
    }

    /** Conflit d'unicité métier : 409. */
    @ExceptionHandler(ConflitRessourceException.class)
    public ResponseEntity<ApiError> gererConflit(ConflitRessourceException ex,
                                                 HttpServletRequest requete) {
        log.info("Conflit de ressource sur {} : {}", requete.getRequestURI(), ex.getMessage());
        return reponse(HttpStatus.CONFLICT, ex.getMessage(), requete);
    }

    /** Construit une réponse d'erreur normalisée avec l'identifiant de corrélation. */
    private ResponseEntity<ApiError> reponse(HttpStatus statut, String message,
                                             HttpServletRequest requete) {
        Object correlation = requete.getAttribute(CorrelationIdFilter.ATTRIBUT_REQUETE);
        ApiError corps = ApiError.de(
                statut.value(),
                statut.getReasonPhrase(),
                message,
                requete.getRequestURI(),
                correlation != null ? correlation.toString() : null);
        return ResponseEntity.status(statut).body(corps);
    }
}
