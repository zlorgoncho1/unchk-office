package sn.unchk.office.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import sn.unchk.office.common.authz.AccesRefuseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire d'exceptions global pour tous les contrôleurs REST des services.
 * <p>
 * Centralise la traduction des exceptions en {@link ApiError} homogènes et SANS fuite :
 * le client ne reçoit jamais de trace de pile ni de message technique interne. Les détails
 * sont journalisés côté serveur, reliés à l'identifiant de corrélation.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Erreurs de validation des DTO (corps de requête annoté {@code @Valid}).
     * Renvoie 400 avec le détail des champs en faute, sans valeur saisie.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> gererValidationCorps(MethodArgumentNotValidException ex,
                                                         HttpServletRequest requete) {
        List<ApiError.ErreurChamp> details = new ArrayList<>();
        for (FieldError erreur : ex.getBindingResult().getFieldErrors()) {
            details.add(new ApiError.ErreurChamp(erreur.getField(), erreur.getDefaultMessage()));
        }
        ApiError corps = ApiError.deValidation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Données de requête invalides.",
                requete.getRequestURI(),
                correlationId(requete),
                details);
        return ResponseEntity.badRequest().body(corps);
    }

    /**
     * Violations de contraintes sur les paramètres (ex : {@code @PathVariable} validé).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> gererViolationContrainte(ConstraintViolationException ex,
                                                            HttpServletRequest requete) {
        List<ApiError.ErreurChamp> details = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            details.add(new ApiError.ErreurChamp(
                    violation.getPropertyPath().toString(),
                    violation.getMessage()));
        }
        ApiError corps = ApiError.deValidation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Paramètres de requête invalides.",
                requete.getRequestURI(),
                correlationId(requete),
                details);
        return ResponseEntity.badRequest().body(corps);
    }

    /**
     * Accès refusé au niveau objet (anti-IDOR, levé par {@link AccesRefuseException}).
     * Renvoie 403 avec un message générique pour ne pas révéler l'existence de la ressource.
     */
    @ExceptionHandler(AccesRefuseException.class)
    public ResponseEntity<ApiError> gererAccesRefuse(AccesRefuseException ex,
                                                     HttpServletRequest requete) {
        log.warn("Accès objet refusé sur {} : {}", requete.getRequestURI(), ex.getMessage());
        return reponseSimple(HttpStatus.FORBIDDEN, "Accès refusé.", requete);
    }

    /**
     * Accès refusé par Spring Security (autorisation de méthode).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> gererAccesRefuseSpring(AccessDeniedException ex,
                                                           HttpServletRequest requete) {
        log.warn("Accès refusé (Spring Security) sur {}", requete.getRequestURI());
        return reponseSimple(HttpStatus.FORBIDDEN, "Accès refusé.", requete);
    }

    /**
     * Argument invalide explicite (règle métier de cohérence).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> gererArgumentInvalide(IllegalArgumentException ex,
                                                          HttpServletRequest requete) {
        log.warn("Requête invalide sur {} : {}", requete.getRequestURI(), ex.getMessage());
        return reponseSimple(HttpStatus.BAD_REQUEST, "Requête invalide.", requete);
    }

    /**
     * Filet de sécurité : toute exception non prévue devient un 500 neutre.
     * Le détail est journalisé (avec la trace) mais jamais renvoyé au client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> gererErreurInattendue(Exception ex,
                                                          HttpServletRequest requete) {
        log.error("Erreur inattendue sur {}", requete.getRequestURI(), ex);
        return reponseSimple(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue.", requete);
    }

    /** Construit une réponse d'erreur simple à partir d'un statut et d'un message neutre. */
    private ResponseEntity<ApiError> reponseSimple(HttpStatus statut, String message,
                                                   HttpServletRequest requete) {
        ApiError corps = ApiError.de(
                statut.value(),
                statut.getReasonPhrase(),
                message,
                requete.getRequestURI(),
                correlationId(requete));
        return ResponseEntity.status(statut).body(corps);
    }

    /** Récupère l'identifiant de corrélation posé par le {@link CorrelationIdFilter}. */
    private String correlationId(HttpServletRequest requete) {
        Object valeur = requete.getAttribute(CorrelationIdFilter.ATTRIBUT_REQUETE);
        return valeur != null ? valeur.toString() : null;
    }
}
