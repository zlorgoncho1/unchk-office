package sn.unchk.office.identity.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.identity.depot.RefreshTokenRepository;
import sn.unchk.office.identity.depot.RoleUtilisateurRepository;
import sn.unchk.office.identity.depot.UtilisateurRepository;
import sn.unchk.office.identity.domaine.RefreshToken;
import sn.unchk.office.identity.domaine.RoleCode;
import sn.unchk.office.identity.domaine.RoleUtilisateur;
import sn.unchk.office.identity.domaine.Utilisateur;
import sn.unchk.office.identity.dto.RequeteCreationUtilisateur;
import sn.unchk.office.identity.dto.RequeteMajUtilisateur;
import sn.unchk.office.identity.dto.VueUtilisateur;
import sn.unchk.office.identity.messaging.ProducteurUtilisateur;
import sn.unchk.office.identity.messaging.UtilisateurEvenement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestion des comptes (CRUD réservé à l'admin).
 * <p>
 * Chaque changement de compte ou de rôle persiste l'état dans la base {@code identity}
 * ET émet l'évènement {@code identity.users} (event-carried state transfer) afin que les
 * autres services maintiennent leurs read-models (qui notifier, révocation, etc.).
 */
@Service
public class ServiceUtilisateur {

    private final UtilisateurRepository depotUtilisateurs;
    private final RoleUtilisateurRepository depotRoles;
    private final RefreshTokenRepository depotRefresh;
    private final PasswordEncoder encodeur;
    private final ProducteurUtilisateur producteur;
    private final AuditLogger auditLogger;

    public ServiceUtilisateur(UtilisateurRepository depotUtilisateurs,
                              RoleUtilisateurRepository depotRoles,
                              RefreshTokenRepository depotRefresh,
                              PasswordEncoder encodeur,
                              ProducteurUtilisateur producteur,
                              AuditLogger auditLogger) {
        this.depotUtilisateurs = depotUtilisateurs;
        this.depotRoles = depotRoles;
        this.depotRefresh = depotRefresh;
        this.encodeur = encodeur;
        this.producteur = producteur;
        this.auditLogger = auditLogger;
    }

    /** Liste tous les comptes (vue publique, sans secret). */
    @Transactional(readOnly = true)
    public List<VueUtilisateur> lister() {
        List<VueUtilisateur> vues = new ArrayList<>();
        for (Utilisateur u : depotUtilisateurs.findAll()) {
            vues.add(VueUtilisateur.depuis(u, depotRoles.findByIdUserId(u.getId())));
        }
        return vues;
    }

    /** Consulte un compte par identifiant. */
    @Transactional(readOnly = true)
    public VueUtilisateur consulter(UUID id) {
        Utilisateur u = chargerOuEchouer(id);
        return VueUtilisateur.depuis(u, depotRoles.findByIdUserId(id));
    }

    /**
     * Crée un compte et lui affecte ses rôles, puis émet {@code identity.users} (Created).
     *
     * @param requete  données de création validées
     * @param createur identifiant de l'admin créateur (pour granted_by)
     */
    @Transactional
    public VueUtilisateur creer(RequeteCreationUtilisateur requete, UUID createur) {
        if (depotUtilisateurs.existsByEmail(requete.email())) {
            throw new ConflitException("Un compte existe déjà pour ce courriel.");
        }

        Utilisateur utilisateur = Utilisateur.creer(
                requete.email(),
                encodeur.encode(requete.motDePasse()),
                requete.fullName(),
                requete.personRef(),
                requete.personKind());
        depotUtilisateurs.save(utilisateur);

        List<RoleUtilisateur> roles = affecterRoles(utilisateur.getId(), requete.roles(), createur);

        auditLogger.succes("CREATION_COMPTE", "user", utilisateur.getId().toString());
        producteur.publierCree(UtilisateurEvenement.depuis(utilisateur, roles));
        return VueUtilisateur.depuis(utilisateur, roles);
    }

    /**
     * Met à jour un compte (nom, activation, verrouillage, rôles) et émet {@code identity.users} (Updated).
     *
     * @param id      identifiant du compte
     * @param requete champs à mettre à jour (partiels)
     * @param auteur  admin auteur de la modification
     */
    @Transactional
    public VueUtilisateur mettreAJour(UUID id, RequeteMajUtilisateur requete, UUID auteur) {
        Utilisateur utilisateur = chargerOuEchouer(id);

        if (requete.fullName() != null) {
            utilisateur.setFullName(requete.fullName());
        }
        if (requete.active() != null) {
            utilisateur.setActive(requete.active());
        }
        if (requete.locked() != null) {
            utilisateur.setLocked(requete.locked());
            if (!requete.locked()) {
                // Déverrouillage : on remet le compteur d'échecs à zéro.
                utilisateur.setFailedAttempts(0);
            }
        }
        utilisateur.toucher();
        depotUtilisateurs.save(utilisateur);

        List<RoleUtilisateur> roles;
        if (requete.roles() != null) {
            depotRoles.deleteByIdUserId(id);
            roles = affecterRoles(id, requete.roles(), auteur);
        } else {
            roles = depotRoles.findByIdUserId(id);
        }

        auditLogger.succes("MAJ_COMPTE", "user", id.toString());
        producteur.publierMisAJour(UtilisateurEvenement.depuis(utilisateur, roles));
        return VueUtilisateur.depuis(utilisateur, roles);
    }

    /**
     * Réinitialise le mot de passe d'un compte (haché en BCrypt) et révoque ses sessions.
     */
    @Transactional
    public void changerMotDePasse(UUID id, String nouveauMotDePasse) {
        Utilisateur utilisateur = chargerOuEchouer(id);
        utilisateur.setPasswordHash(encodeur.encode(nouveauMotDePasse));
        utilisateur.toucher();
        depotUtilisateurs.save(utilisateur);

        // Sécurité : on révoque tous les refresh tokens existants après changement de mot de passe.
        List<RefreshToken> jetons = depotRefresh.findByUserId(id);
        jetons.forEach(RefreshToken::revoquer);
        depotRefresh.saveAll(jetons);

        auditLogger.succes("RESET_MOT_DE_PASSE", "user", id.toString());
    }

    /**
     * Supprime (logiquement) un compte : désactivé, révoqué, daté ; émet {@code identity.users} (Deleted).
     */
    @Transactional
    public void supprimer(UUID id) {
        Utilisateur utilisateur = chargerOuEchouer(id);
        utilisateur.setActive(false);
        utilisateur.setDeletedAt(Instant.now());
        utilisateur.toucher();
        depotUtilisateurs.save(utilisateur);

        // Révocation de toutes les sessions du compte supprimé.
        List<RefreshToken> jetons = depotRefresh.findByUserId(id);
        jetons.forEach(RefreshToken::revoquer);
        depotRefresh.saveAll(jetons);

        List<RoleUtilisateur> roles = depotRoles.findByIdUserId(id);
        auditLogger.succes("SUPPRESSION_COMPTE", "user", id.toString());
        producteur.publierSupprime(UtilisateurEvenement.depuis(utilisateur, roles));
    }

    /** Charge un compte ou lève une exception 404. */
    private Utilisateur chargerOuEchouer(UUID id) {
        return depotUtilisateurs.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte introuvable."));
    }

    /** Affecte une liste de rôles (libellés) à un utilisateur et renvoie les affectations. */
    private List<RoleUtilisateur> affecterRoles(UUID userId, List<String> libelles, UUID createur) {
        List<RoleUtilisateur> affectations = new ArrayList<>();
        for (String libelle : libelles) {
            RoleCode role = RoleCode.depuisLibelle(libelle);
            affectations.add(RoleUtilisateur.creer(userId, role, createur));
        }
        return depotRoles.saveAll(affectations);
    }
}
