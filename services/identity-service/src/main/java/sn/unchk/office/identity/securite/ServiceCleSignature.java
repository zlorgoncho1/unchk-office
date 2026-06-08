package sn.unchk.office.identity.securite;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.identity.depot.CleSignatureRepository;
import sn.unchk.office.identity.domaine.CleSignature;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion des clés de signature RSA (génération, chargement, rotation, exposition JWKS).
 * <p>
 * Au démarrage, garantit qu'une clé active existe : si la table {@code signing_keys} est vide,
 * une paire RSA 2048 bits est générée et persistée. La clé active sert à signer les JWT ;
 * toutes les clés (actives ou non) sont exposées via JWKS afin que les jetons déjà émis
 * restent vérifiables après rotation.
 */
@Service
public class ServiceCleSignature {

    private static final Logger log = LoggerFactory.getLogger(ServiceCleSignature.class);

    /** Taille des clés RSA : 2048 bits (compromis sécurité/performance pour RS256). */
    private static final int TAILLE_CLE = 2048;

    private final CleSignatureRepository depot;

    public ServiceCleSignature(CleSignatureRepository depot) {
        this.depot = depot;
    }

    /**
     * S'assure qu'au moins une clé active existe ; en génère une sinon.
     * Appelée au démarrage via {@code DemarrageIdentity}.
     */
    @Transactional
    public CleSignature assurerCleActive() {
        return depot.findFirstByActiveTrue().orElseGet(this::genererEtEnregistrer);
    }

    /** Génère une nouvelle paire RSA, la persiste comme clé active et la renvoie. */
    @Transactional
    public CleSignature genererEtEnregistrer() {
        KeyPair paire = genererPaire();
        String kid = UUID.randomUUID().toString();
        String publicPem = PemUtil.versPem(paire.getPublic());
        String privatePem = PemUtil.versPem(paire.getPrivate());
        CleSignature cle = CleSignature.creer(kid, publicPem, privatePem);
        CleSignature enregistree = depot.save(cle);
        log.info("Nouvelle clé de signature RSA générée (kid={})", kid);
        return enregistree;
    }

    /**
     * Effectue une rotation : désactive la clé active courante et en génère une nouvelle.
     * Les anciennes clés publiques restent exposées via JWKS pour valider les jetons en cours.
     */
    @Transactional
    public CleSignature rotation() {
        depot.findFirstByActiveTrue().ifPresent(active -> {
            active.desactiver();
            depot.save(active);
            log.info("Rotation : ancienne clé désactivée (kid={})", active.getKid());
        });
        return genererEtEnregistrer();
    }

    /** Renvoie la clé active courante, en la créant si nécessaire (robustesse). */
    @Transactional(readOnly = true)
    public CleSignature cleActive() {
        return depot.findFirstByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Aucune clé de signature active"));
    }

    /**
     * Construit le document JWKS (clés publiques uniquement) à exposer sur
     * {@code /.well-known/jwks.json}. La clé privée n'y figure JAMAIS.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> jwks() {
        List<RSAKey> cles = new ArrayList<>();
        for (CleSignature cle : depot.findAll()) {
            RSAPublicKey publique = (RSAPublicKey) PemUtil.clePubliqueDepuisPem(cle.getPublicPem());
            RSAKey rsaKey = new RSAKey.Builder(publique)
                    .keyID(cle.getKid())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .build();
            cles.add(rsaKey);
        }
        // toJSONObject() ne sérialise que la partie publique des clés.
        return new JWKSet(new ArrayList<>(cles)).toJSONObject();
    }

    /** Charge la clé privée RSA d'une clé de signature (pour signer les jetons). */
    public RSAPrivateKey clePrivee(CleSignature cle) {
        return (RSAPrivateKey) PemUtil.clePriveeDepuisPem(cle.getPrivatePem());
    }

    /** Charge la clé publique RSA (pour la validation locale des jetons). */
    public RSAPublicKey clePublique(CleSignature cle) {
        return (RSAPublicKey) PemUtil.clePubliqueDepuisPem(cle.getPublicPem());
    }

    private KeyPair genererPaire() {
        try {
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
            generateur.initialize(TAILLE_CLE);
            return generateur.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Échec de génération de la paire RSA", ex);
        }
    }
}
