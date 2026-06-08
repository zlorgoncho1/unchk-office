package sn.unchk.office.identity.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Extraction d'informations de la requête HTTP pour l'audit (IP source, agent client).
 */
public final class RequeteHttpUtil {

    private RequeteHttpUtil() {
        // Classe utilitaire.
    }

    /**
     * Détermine l'adresse IP source en tenant compte d'un éventuel proxy/gateway en amont.
     * On ne garde que la première IP de {@code X-Forwarded-For} si présente.
     */
    public static String adresseIp(HttpServletRequest requete) {
        String forwarded = requete.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return requete.getRemoteAddr();
    }

    /** Renvoie l'agent client (tronqué pour éviter un stockage abusif). */
    public static String userAgent(HttpServletRequest requete) {
        String agent = requete.getHeader("User-Agent");
        if (agent == null) {
            return null;
        }
        return agent.length() > 512 ? agent.substring(0, 512) : agent;
    }
}
