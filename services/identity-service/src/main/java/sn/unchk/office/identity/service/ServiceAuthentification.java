package sn.unchk.office.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.identity.depot.AuditAuthRepository;
import sn.unchk.office.identity.depot.RefreshTokenRepository;
import sn.unchk.office.identity.depot.RoleUtilisateurRepository;
import sn.unchk.office.identity.depot.UtilisateurRepository;
import sn.unchk.office.identity.domaine.AuditAuth;
import sn.unchk.office.identity.domaine.CleSignature;
import sn.unchk.office.identity.domaine.RefreshToken;
import sn.unchk.office.identity.domaine.RoleUtilisateur;
import sn.unchk.office.identity.domaine.Utilisateur;
import sn.unchk.office.identity.dto.ReponseJetons;
import sn.unchk.office.identity.messaging.ProducteurUtilisateur;
import sn.unchk.office.identity.messaging.UtilisateurEvenement;
import sn.unchk.office.identity.securite.SecuriteProprietes;
import sn.unchk.office.identity.securite.ServiceCleSignature;
import sn.unchk.office.identity.securite.ServiceJwt;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

/**
 * Logique d'authentification : connexion (BCrypt), rafraîchissement, déconnexion.
 * <p>
 * Applique le durcissement OWASP A07 : verrouillage après N échecs, audit systématique
 * (LOGIN_OK / LOGIN_FAIL / LOGOUT...), messages d'erreur génériques (anti-énumération),
 * refresh tokens stockés en hash et révocables. Émet {@code identity.users} quand le statut
 * du compte change (ex : verrouillage anti-bruteforce).
 */
@Service
public class ServiceAuthentification {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthentification.class);

    /** Générateur sécurisé pour les refresh tokens (valeur aléatoire forte). */
    private static final SecureRandom ALEA = new SecureRandom();

    private final UtilisateurRepository depotUtilisateurs;
    private final RoleUtilisateurRepository depotRoles;
    private final RefreshTokenRepository depotRefresh;
    private final AuditAuthRepository depotAudit;
    private final PasswordEncoder encodeur;
    private final ServiceJwt serviceJwt;
    private final ServiceCleSignature serviceCle;
    private final ProducteurUtilisateur producteur;
    private final SecuriteProprietes securiteProprietes;

    public ServiceAuthentification(UtilisateurRepository depotUtilisateurs,
                                   RoleUtilisateurRepository depotRoles,
                                   RefreshTokenRepository depotRefresh,
                                   AuditAuthRepository depotAudit,
                                   PasswordEncoder encodeur,
                                   ServiceJwt serviceJwt,
                                   ServiceCleSignature serviceCle,
                                   ProducteurUtilisateur producteur,
                                   SecuriteProprietes securiteProprietes) {
        this.depotUtilisateurs = depotUtilisateurs;
        this.depotRoles = depotRoles;
        this.depotRefresh = depotRefresh;
        this.depotAudit = depotAudit;
        this.encodeur = encodeur;
        this.serviceJwt = serviceJwt;
        this.serviceCle = serviceCle;
        this.producteur = producteur;
        this.securiteProprietes = securiteProprietes;
    }

    /**
     * Authentifie un utilisateur par email + mot de passe et délivre une paire de jetons.
     *
     * @param email     courriel saisi
     * @param motDePasse mot de passe en clair
     * @param ip        adresse IP source (audit)
     * @param userAgent agent client (audit)
     * @return access token + refresh token
     * @throws AuthentificationException en cas d'échec (message générique)
     */
    @Transactional
    public ReponseJetons connexion(String email, String motDePasse, String ip, String userAgent) {
        Utilisateur utilisateur = depotUtilisateurs.findByEmail(email).orElse(null);

        // Compte inconnu : on audite et on renvoie un message générique (anti-énumération).
        if (utilisateur == null) {
            auditer(null, "LOGIN_FAIL", ip, userAgent);
            throw new AuthentificationException("Compte inconnu");
        }

        // Compte non utilisable (désactivé/verrouillé/supprimé) : refus.
        if (!utilisateur.estUtilisable()) {
            auditer(utilisateur.getId(), "LOGIN_FAIL", ip, userAgent);
            throw new AuthentificationException("Compte non utilisable");
        }

        // Mot de passe erroné : on incrémente le compteur d'échecs (et on verrouille au seuil).
        if (!encodeur.matches(motDePasse, utilisateur.getPasswordHash())) {
            boolean etaitVerrouille = utilisateur.isLocked();
            utilisateur.connexionEchouee(securiteProprietes.maxEchecs());
            depotUtilisateurs.save(utilisateur);
            auditer(utilisateur.getId(), "LOGIN_FAIL", ip, userAgent);
            if (!etaitVerrouille && utilisateur.isLocked()) {
                // Le verrouillage change le statut : on le propage et on l'audite.
                auditer(utilisateur.getId(), "LOCK", ip, userAgent);
                publierEtat(utilisateur);
            }
            throw new AuthentificationException("Mot de passe invalide");
        }

        // Succès : remise à zéro des échecs, audit, émission des jetons.
        utilisateur.connexionReussie();
        depotUtilisateurs.save(utilisateur);
        auditer(utilisateur.getId(), "LOGIN_OK", ip, userAgent);

        return delivrerJetons(utilisateur);
    }

    /**
     * Échange un refresh token valide contre une nouvelle paire de jetons (rotation).
     *
     * @param refreshTokenBrut refresh token présenté par le client
     * @param ip               adresse IP (audit)
     * @param userAgent        agent client (audit)
     * @return nouveaux jetons
     * @throws AuthentificationException si le jeton est inconnu, expiré ou révoqué
     */
    @Transactional
    public ReponseJetons rafraichir(String refreshTokenBrut, String ip, String userAgent) {
        String hash = HashUtil.sha256(refreshTokenBrut);
        RefreshToken jeton = depotRefresh.findByTokenHash(hash).orElse(null);

        if (jeton == null || !jeton.estValide()) {
            auditer(jeton != null ? jeton.getUserId() : null, "REFRESH_FAIL", ip, userAgent);
            throw new AuthentificationException("Refresh token invalide");
        }

        Utilisateur utilisateur = depotUtilisateurs.findById(jeton.getUserId()).orElse(null);
        if (utilisateur == null || !utilisateur.estUtilisable()) {
            throw new AuthentificationException("Compte non utilisable");
        }

        // Rotation : on révoque l'ancien jeton et on en délivre un nouveau (anti-rejeu).
        jeton.revoquer();
        depotRefresh.save(jeton);
        auditer(utilisateur.getId(), "REFRESH_OK", ip, userAgent);

        return delivrerJetons(utilisateur);
    }

    /**
     * Déconnexion : révoque le refresh token fourni (et tous ceux de l'utilisateur).
     *
     * @param refreshTokenBrut refresh token à révoquer
     * @param ip               adresse IP (audit)
     * @param userAgent        agent client (audit)
     */
    @Transactional
    public void deconnexion(String refreshTokenBrut, String ip, String userAgent) {
        String hash = HashUtil.sha256(refreshTokenBrut);
        depotRefresh.findByTokenHash(hash).ifPresent(jeton -> {
            // On révoque tous les jetons de l'utilisateur pour invalider toutes ses sessions.
            List<RefreshToken> jetons = depotRefresh.findByUserId(jeton.getUserId());
            jetons.forEach(RefreshToken::revoquer);
            depotRefresh.saveAll(jetons);
            auditer(jeton.getUserId(), "LOGOUT", ip, userAgent);
        });
    }

    /** Construit la réponse jetons : access RS256 signé + refresh persisté (hash). */
    private ReponseJetons delivrerJetons(Utilisateur utilisateur) {
        List<RoleUtilisateur> roles = depotRoles.findByIdUserId(utilisateur.getId());
        List<String> libelles = roles.stream().map(r -> r.getRole().libelle()).toList();

        CleSignature cle = serviceCle.cleActive();
        String accessToken = serviceJwt.emettreAccessToken(
                utilisateur, libelles, cle, serviceCle.clePrivee(cle));

        String refreshBrut = genererRefreshToken();
        Instant expiration = Instant.now().plus(serviceJwt.dureeRefreshJours(), ChronoUnit.DAYS);
        depotRefresh.save(RefreshToken.creer(
                utilisateur.getId(), HashUtil.sha256(refreshBrut), expiration));

        return ReponseJetons.bearer(
                accessToken,
                refreshBrut,
                serviceJwt.dureeAccessSecondes(),
                utilisateur.getId().toString(),
                libelles);
    }

    /** Génère un refresh token opaque : 32 octets aléatoires encodés en base64 URL. */
    private String genererRefreshToken() {
        byte[] octets = new byte[32];
        ALEA.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    /** Publie l'état courant du compte sur identity.users (changement de statut). */
    private void publierEtat(Utilisateur utilisateur) {
        List<RoleUtilisateur> roles = depotRoles.findByIdUserId(utilisateur.getId());
        producteur.publierMisAJour(UtilisateurEvenement.depuis(utilisateur, roles));
    }

    /** Écrit une ligne dans le journal d'authentification (table auth_audit). */
    private void auditer(java.util.UUID userId, String event, String ip, String userAgent) {
        try {
            depotAudit.save(AuditAuth.creer(userId, event, ip, userAgent));
        } catch (RuntimeException ex) {
            // L'audit ne doit jamais faire échouer l'authentification : on journalise seulement.
            log.warn("Échec d'écriture de l'audit d'authentification ({})", event, ex);
        }
    }
}
