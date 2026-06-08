package sn.unchk.office.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre d'identifiant de corrélation.
 * <p>
 * Lit l'en-tête {@code X-Correlation-Id} entrant (généralement posé par le gateway) ou en
 * génère un nouveau. L'identifiant est :
 * <ul>
 *   <li>placé dans le MDC du logger (clé {@code correlationId}) pour tracer tous les logs ;</li>
 *   <li>exposé en attribut de requête (réutilisé par le {@link GlobalExceptionHandler}) ;</li>
 *   <li>recopié dans la réponse pour permettre au client de citer l'identifiant en cas d'incident.</li>
 * </ul>
 * Exécuté très tôt dans la chaîne pour couvrir l'ensemble du traitement.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Nom de l'en-tête de corrélation, partagé avec le gateway. */
    public static final String ENTETE_CORRELATION = "X-Correlation-Id";

    /** Clé MDC pour faire apparaître l'identifiant dans chaque ligne de log. */
    public static final String CLE_MDC = "correlationId";

    /** Attribut de requête exposant l'identifiant aux autres composants (handler d'erreur). */
    public static final String ATTRIBUT_REQUETE = "unchk.correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest requete,
                                    HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {
        String correlationId = requete.getHeader(ENTETE_CORRELATION);
        if (!StringUtils.hasText(correlationId)) {
            // Aucun identifiant fourni : on en crée un (UUID, non devinable).
            correlationId = UUID.randomUUID().toString();
        }
        try {
            MDC.put(CLE_MDC, correlationId);
            requete.setAttribute(ATTRIBUT_REQUETE, correlationId);
            reponse.setHeader(ENTETE_CORRELATION, correlationId);
            chaine.doFilter(requete, reponse);
        } finally {
            // Nettoyage indispensable : le thread est réutilisé dans le pool servlet.
            MDC.remove(CLE_MDC);
        }
    }
}
