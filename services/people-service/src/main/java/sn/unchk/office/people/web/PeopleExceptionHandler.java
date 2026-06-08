package sn.unchk.office.people.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sn.unchk.office.common.web.ApiError;
import sn.unchk.office.common.web.CorrelationIdFilter;
import sn.unchk.office.people.service.ConflitDonneesException;
import sn.unchk.office.people.service.RessourceIntrouvableException;

/**
 * Gestionnaire d'exceptions specifique au people-service.
 * <p>
 * Complete le {@code GlobalExceptionHandler} de la librairie commune avec les exceptions
 * metier propres au service (404 ressource introuvable, 409 conflit). Place avant le
 * gestionnaire generique ({@link Order}) pour que ces cas ne tombent pas dans le filet 500.
 * Les corps suivent la structure {@link ApiError} sans fuite d'information interne.
 */
@RestControllerAdvice
@Order(0)
public class PeopleExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PeopleExceptionHandler.class);

    /**
     * Ressource introuvable (ou non autorisee en lecture) : 404 sobre.
     * Indistinct d'un refus d'acces pour ne pas confirmer l'existence d'un UUID.
     */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiError> gererIntrouvable(RessourceIntrouvableException ex,
                                                     HttpServletRequest requete) {
        return reponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", requete);
    }

    /** Conflit de donnees metier (INE/matricule deja utilise) : 409. */
    @ExceptionHandler(ConflitDonneesException.class)
    public ResponseEntity<ApiError> gererConflit(ConflitDonneesException ex,
                                                 HttpServletRequest requete) {
        log.info("Conflit de donnees sur {} : {}", requete.getRequestURI(), ex.getMessage());
        return reponse(HttpStatus.CONFLICT, "Conflit : la donnee existe deja.", requete);
    }

    /** Violation de contrainte d'unicite cote base : 409 (filet de securite). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> gererIntegrite(DataIntegrityViolationException ex,
                                                   HttpServletRequest requete) {
        log.warn("Violation d'integrite sur {}", requete.getRequestURI());
        return reponse(HttpStatus.CONFLICT, "Conflit de donnees.", requete);
    }

    /** Conflit de verrouillage optimiste (version) : 409. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> gererVerrou(OptimisticLockingFailureException ex,
                                                HttpServletRequest requete) {
        log.info("Conflit de version sur {}", requete.getRequestURI());
        return reponse(HttpStatus.CONFLICT,
                "La ressource a ete modifiee entre-temps, veuillez reessayer.", requete);
    }

    private ResponseEntity<ApiError> reponse(HttpStatus statut, String message,
                                             HttpServletRequest requete) {
        Object correlationId = requete.getAttribute(CorrelationIdFilter.ATTRIBUT_REQUETE);
        ApiError corps = ApiError.de(
                statut.value(),
                statut.getReasonPhrase(),
                message,
                requete.getRequestURI(),
                correlationId != null ? correlationId.toString() : null);
        return ResponseEntity.status(statut).body(corps);
    }
}
