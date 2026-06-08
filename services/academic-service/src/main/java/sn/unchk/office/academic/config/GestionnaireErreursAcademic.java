package sn.unchk.office.academic.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import sn.unchk.office.academic.formation.CodeFormationDejaUtiliseException;
import sn.unchk.office.academic.formation.FormationIntrouvableException;
import sn.unchk.office.common.web.ApiError;

/**
 * Gestionnaire d'erreurs propre au academic-service, pour les exceptions métier non couvertes
 * par le gestionnaire global de la librairie commune.
 * <p>
 * Placé avant le gestionnaire global ({@code @Order(HIGHEST_PRECEDENCE)}) afin que ces
 * traductions précises priment. Les réponses restent sobres (aucune fuite d'interne).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GestionnaireErreursAcademic {

    /**
     * Formation introuvable : 404 (anti-énumération — on ne confirme pas l'existence d'un UUID).
     */
    @ExceptionHandler(FormationIntrouvableException.class)
    public ResponseEntity<ApiError> gererIntrouvable(HttpServletRequest requete) {
        return reponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", requete);
    }

    /**
     * Conflit d'unicité du code de formation : 409.
     */
    @ExceptionHandler(CodeFormationDejaUtiliseException.class)
    public ResponseEntity<ApiError> gererConflitCode(HttpServletRequest requete) {
        return reponse(HttpStatus.CONFLICT, "Le code de formation est déjà utilisé.", requete);
    }

    /** Construit une réponse d'erreur sobre à partir d'un statut et d'un message neutre. */
    private ResponseEntity<ApiError> reponse(HttpStatus statut, String message, HttpServletRequest requete) {
        ApiError corps = ApiError.de(
                statut.value(),
                statut.getReasonPhrase(),
                message,
                requete.getRequestURI(),
                null);
        return ResponseEntity.status(statut).body(corps);
    }
}
