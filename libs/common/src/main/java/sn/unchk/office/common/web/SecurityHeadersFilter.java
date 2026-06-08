package sn.unchk.office.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre ajoutant les en-têtes de sécurité HTTP (durcissement OWASP).
 * <p>
 * Même si le gateway pose déjà ces en-têtes en première ligne, les services métier les
 * réappliquent en défense en profondeur (au cas où un service serait exposé directement).
 * Ces en-têtes réduisent les risques de clickjacking, de sniffing de type MIME et de fuite
 * de référent.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest requete,
                                    HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {
        // Empêche l'interprétation MIME différente de celle déclarée (anti sniffing).
        reponse.setHeader("X-Content-Type-Options", "nosniff");
        // Interdit l'affichage du service dans une iframe (anti clickjacking).
        reponse.setHeader("X-Frame-Options", "DENY");
        // Ne divulgue pas l'URL d'origine vers des tiers.
        reponse.setHeader("Referrer-Policy", "no-referrer");
        // Politique de contenu stricte : ce sont des API JSON, aucun contenu actif attendu.
        reponse.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        // Force HTTPS pendant un an (inclut les sous-domaines).
        reponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        // Désactive des API navigateur sensibles non nécessaires à une API.
        reponse.setHeader("Permissions-Policy", "geolocation=(), camera=(), microphone=()");

        chaine.doFilter(requete, reponse);
    }
}
