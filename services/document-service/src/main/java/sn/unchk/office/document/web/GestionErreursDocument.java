package sn.unchk.office.document.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import sn.unchk.office.common.web.ApiError;
import sn.unchk.office.common.web.CorrelationIdFilter;
import sn.unchk.office.document.service.RessourceIntrouvableException;

/**
 * Gestion d'erreurs propre au document-service (complète celle de la librairie commune).
 * <p>
 * Priorité haute pour intercepter les cas spécifiques (404, dépassement de taille d'upload)
 * avant le filet de sécurité générique du {@code GlobalExceptionHandler} commun. Les réponses
 * restent sobres (anti-fuite, anti-énumération).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GestionErreursDocument {

    private static final Logger log = LoggerFactory.getLogger(GestionErreursDocument.class);

    /**
     * Document introuvable (ou refus en lecture rendu indistinct) : 404 sobre.
     */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiError> gererIntrouvable(RessourceIntrouvableException ex,
                                                     HttpServletRequest requete) {
        // Message neutre : ne confirme pas l'existence d'un UUID (anti-énumération).
        return reponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", requete);
    }

    /**
     * Fichier trop volumineux (limite multipart) : 413.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> gererTropVolumineux(MaxUploadSizeExceededException ex,
                                                        HttpServletRequest requete) {
        log.warn("Upload refusé (taille dépassée) sur {}", requete.getRequestURI());
        return reponse(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux.", requete);
    }

    private ResponseEntity<ApiError> reponse(HttpStatus statut, String message,
                                             HttpServletRequest requete) {
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
