package sn.unchk.office.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Garde-fous d'upload : taille maximale et liste blanche de types MIME (durcissement OWASP).
 * <p>
 * Renseignées sous le préfixe {@code document.upload}.
 *
 * @param tailleMaxOctets    taille maximale autorisée d'un fichier (octets)
 * @param typesMimeAutorises liste blanche des types MIME acceptés
 */
@ConfigurationProperties(prefix = "document.upload")
public record UploadProprietes(
        long tailleMaxOctets,
        List<String> typesMimeAutorises
) {

    public UploadProprietes {
        if (tailleMaxOctets <= 0) {
            // Repli : 25 Mo.
            tailleMaxOctets = 26214400L;
        }
        if (typesMimeAutorises == null) {
            typesMimeAutorises = List.of();
        }
    }

    /** Vrai si le type MIME fait partie de la liste blanche. */
    public boolean estMimeAutorise(String mimeType) {
        return mimeType != null && typesMimeAutorises.contains(mimeType);
    }
}
