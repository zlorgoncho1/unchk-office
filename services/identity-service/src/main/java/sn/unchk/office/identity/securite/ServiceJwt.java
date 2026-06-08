package sn.unchk.office.identity.securite;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import sn.unchk.office.identity.domaine.CleSignature;
import sn.unchk.office.identity.domaine.Utilisateur;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Émission des JWT RS256 signés par la clé active.
 * <p>
 * Le jeton porte le sujet ({@code sub} = UUID utilisateur), les rôles ({@code roles}),
 * l'émetteur ({@code iss}), l'audience ({@code aud}) et l'expiration ({@code exp}). L'en-tête
 * inclut le {@code kid} de la clé pour que les valideurs retrouvent la clé publique via JWKS.
 * Aucun secret ni hash de mot de passe n'est jamais placé dans le jeton.
 */
@Service
public class ServiceJwt {

    private final JwtEmissionProprietes proprietes;

    public ServiceJwt(JwtEmissionProprietes proprietes) {
        this.proprietes = proprietes;
    }

    /**
     * Forge un access token RS256 pour l'utilisateur et ses rôles.
     *
     * @param utilisateur compte authentifié
     * @param roles       libellés des rôles (ex : "enseignant")
     * @param cle         clé de signature active (fournit le kid et la clé privée)
     * @param clePrivee   clé privée RSA correspondante
     * @return jeton compact signé (header.payload.signature)
     */
    public String emettreAccessToken(Utilisateur utilisateur, List<String> roles,
                                     CleSignature cle, java.security.interfaces.RSAPrivateKey clePrivee) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(proprietes.accessTtlMin(), ChronoUnit.MINUTES);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(utilisateur.getId().toString())
                .issuer(proprietes.issuer())
                .audience(proprietes.audience())
                .issueTime(Date.from(maintenant))
                .expirationTime(Date.from(expiration))
                .claim("roles", roles)
                .claim("email", utilisateur.getEmail())
                .claim("name", utilisateur.getFullName())
                .build();

        JWSHeader entete = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(cle.getKid())
                .type(com.nimbusds.jose.JOSEObjectType.JWT)
                .build();

        SignedJWT jwt = new SignedJWT(entete, claims);
        try {
            jwt.sign(new RSASSASigner(clePrivee));
        } catch (JOSEException ex) {
            throw new IllegalStateException("Échec de signature du JWT", ex);
        }
        return jwt.serialize();
    }

    /** Durée de vie de l'access token en secondes (renvoyée au client). */
    public long dureeAccessSecondes() {
        return proprietes.accessTtlMin() * 60L;
    }

    /** Durée de vie du refresh token en jours (utilisée pour calculer son expiration). */
    public int dureeRefreshJours() {
        return proprietes.refreshTtlDays();
    }
}
