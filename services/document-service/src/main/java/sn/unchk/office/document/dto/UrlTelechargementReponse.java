package sn.unchk.office.document.dto;

import java.time.Instant;

/**
 * URL présignée de téléchargement d'un binaire MinIO.
 * <p>
 * Le service ne diffuse jamais le binaire lui-même : il délivre une URL temporaire,
 * délivrée uniquement après contrôle d'accès OPA au niveau objet (anti-IDOR).
 *
 * @param url       URL présignée valable un temps limité
 * @param expireA   instant d'expiration de l'URL
 * @param fileName  nom de fichier suggéré au navigateur
 * @param mimeType  type MIME du binaire
 */
public record UrlTelechargementReponse(
        String url,
        Instant expireA,
        String fileName,
        String mimeType
) {
}
