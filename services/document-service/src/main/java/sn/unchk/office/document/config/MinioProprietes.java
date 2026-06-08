package sn.unchk.office.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de connexion à MinIO et des buckets utilisés par le service.
 * <p>
 * Renseignées sous le préfixe {@code document.minio} dans {@code application.yml}.
 *
 * @param endpoint             URL de l'API MinIO (ex : {@code http://minio:9000})
 * @param accessKey            identifiant d'accès
 * @param secretKey            clé secrète
 * @param bucketDefaut         bucket par défaut (documents génériques)
 * @param bucketCourriers      bucket dédié au courrier
 * @param presignedTtlSecondes durée de validité des URLs présignées (en secondes)
 */
@ConfigurationProperties(prefix = "document.minio")
public record MinioProprietes(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucketDefaut,
        String bucketCourriers,
        int presignedTtlSecondes
) {

    public MinioProprietes {
        // Valeurs de repli si certaines propriétés sont absentes.
        if (bucketDefaut == null || bucketDefaut.isBlank()) {
            bucketDefaut = "documents";
        }
        if (bucketCourriers == null || bucketCourriers.isBlank()) {
            bucketCourriers = "courriers";
        }
        if (presignedTtlSecondes <= 0) {
            presignedTtlSecondes = 300;
        }
    }
}
