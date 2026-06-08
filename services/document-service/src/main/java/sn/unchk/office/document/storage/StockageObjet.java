package sn.unchk.office.document.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sn.unchk.office.document.config.MinioProprietes;

import java.io.InputStream;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Adaptateur du stockage objet MinIO : dépose, supprime et génère des URLs présignées.
 * <p>
 * Le binaire ne transite jamais par Kafka ni n'est stocké en base : seules les métadonnées
 * et le couple {@code (bucket, clé objet)} le sont. Le téléchargement s'effectue via une URL
 * présignée temporaire, délivrée uniquement après contrôle d'accès OPA (anti-IDOR).
 */
@Component
public class StockageObjet {

    private static final Logger log = LoggerFactory.getLogger(StockageObjet.class);

    private final MinioClient minioClient;
    private final MinioProprietes proprietes;

    public StockageObjet(MinioClient minioClient, MinioProprietes proprietes) {
        this.minioClient = minioClient;
        this.proprietes = proprietes;
    }

    /**
     * Dépose un binaire dans le bucket donné.
     *
     * @param bucket    bucket cible (documents, courriers, ...)
     * @param objectKey clé objet (chemin S3) — doit être unique dans le bucket
     * @param flux      flux du binaire
     * @param taille    taille en octets (connue d'avance, ici via le MultipartFile)
     * @param mimeType  type MIME du binaire
     */
    public void deposer(String bucket, String objectKey, InputStream flux,
                        long taille, String mimeType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(flux, taille, -1)
                    .contentType(mimeType)
                    .build());
        } catch (Exception ex) {
            log.error("Échec du dépôt MinIO bucket={} objet={}", bucket, objectKey, ex);
            throw new StockageException("Dépôt du binaire impossible.", ex);
        }
    }

    /**
     * Supprime un binaire du stockage (best-effort : un échec ne doit pas casser le flux métier).
     */
    public void supprimer(String bucket, String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            // On journalise mais on n'interrompt pas : la métadonnée a déjà été supprimée.
            log.warn("Suppression MinIO impossible bucket={} objet={}", bucket, objectKey, ex);
        }
    }

    /**
     * Génère une URL présignée de téléchargement (GET), valable un temps limité.
     *
     * @param bucket    bucket source
     * @param objectKey clé objet
     * @return URL présignée
     */
    public String urlTelechargement(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(proprietes.presignedTtlSecondes(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception ex) {
            log.error("Génération d'URL présignée impossible bucket={} objet={}", bucket, objectKey, ex);
            throw new StockageException("Génération du lien de téléchargement impossible.", ex);
        }
    }

    /** Instant d'expiration d'une URL fraîchement générée (selon le TTL configuré). */
    public Instant instantExpiration() {
        return Instant.now().plusSeconds(proprietes.presignedTtlSecondes());
    }
}
