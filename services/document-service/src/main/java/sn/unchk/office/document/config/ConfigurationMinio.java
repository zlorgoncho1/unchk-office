package sn.unchk.office.document.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du client MinIO (SDK officiel) et des propriétés associées.
 * <p>
 * Le client est instancié une fois et réutilisé : dépôt, suppression et génération
 * d'URLs présignées de téléchargement.
 */
@Configuration
@EnableConfigurationProperties({MinioProprietes.class, UploadProprietes.class})
public class ConfigurationMinio {

    /**
     * Client MinIO connecté à l'endpoint configuré avec les identifiants fournis.
     */
    @Bean
    public MinioClient minioClient(MinioProprietes proprietes) {
        return MinioClient.builder()
                .endpoint(proprietes.endpoint())
                .credentials(proprietes.accessKey(), proprietes.secretKey())
                .build();
    }
}
