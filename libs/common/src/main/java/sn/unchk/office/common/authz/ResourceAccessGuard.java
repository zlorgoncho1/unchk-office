package sn.unchk.office.common.authz;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * Garde d'accès au niveau objet (anti-IDOR).
 * <p>
 * Aspect déclenché par l'annotation {@link VerifieAccesObjet}. Avant l'exécution de la
 * méthode protégée, il :
 * <ol>
 *   <li>récupère le sujet courant (id + rôles) depuis le SecurityContext ;</li>
 *   <li>résout l'identifiant de la ressource depuis le paramètre désigné par {@code idParam} ;</li>
 *   <li>enrichit éventuellement la ressource via {@link FournisseurAttributsRessource} ;</li>
 *   <li>interroge OPA et lève {@link AccesRefuseException} si l'accès est refusé.</li>
 * </ol>
 * Ainsi un utilisateur ne peut accéder à un objet dont il devine l'UUID que si OPA l'autorise.
 */
@Aspect
public class ResourceAccessGuard {

    private static final Logger log = LoggerFactory.getLogger(ResourceAccessGuard.class);

    private final OpaClient opaClient;
    /** Optionnel : chaque service peut fournir un enrichisseur d'attributs ABAC. */
    private final ObjectProvider<FournisseurAttributsRessource> fournisseur;

    public ResourceAccessGuard(OpaClient opaClient,
                               ObjectProvider<FournisseurAttributsRessource> fournisseur) {
        this.opaClient = opaClient;
        this.fournisseur = fournisseur;
    }

    /**
     * Intercepte toute méthode annotée {@link VerifieAccesObjet} et vérifie l'accès avant exécution.
     */
    @Around("@annotation(annotation)")
    public Object verifier(ProceedingJoinPoint pjp, VerifieAccesObjet annotation) throws Throwable {
        String idRessource = resoudreIdentifiant(pjp, annotation.idParam());
        EntreeOpa.Sujet sujet = ContexteSecurite.sujetCourant();
        EntreeOpa.Ressource ressource = construireRessource(annotation.type(), idRessource);
        EntreeOpa.Requete requete = requeteCourante();

        EntreeOpa entree = new EntreeOpa(sujet, annotation.action(), ressource, requete);

        if (!opaClient.estAutorise(entree)) {
            // Message volontairement générique : ne révèle pas l'existence de la ressource.
            log.warn("Accès objet refusé : type={} action={} sujet={}",
                    annotation.type(), annotation.action(), sujet.id());
            throw new AccesRefuseException("Accès refusé à la ressource demandée.");
        }
        // Autorisé : on exécute la méthode protégée.
        return pjp.proceed();
    }

    /**
     * Lit la valeur du paramètre nommé {@code idParam} dans les arguments de la méthode.
     */
    private String resoudreIdentifiant(ProceedingJoinPoint pjp, String idParam) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] noms = signature.getParameterNames();
        Object[] valeurs = pjp.getArgs();
        if (noms != null) {
            for (int i = 0; i < noms.length; i++) {
                if (idParam.equals(noms[i]) && valeurs[i] != null) {
                    return valeurs[i].toString();
                }
            }
        }
        // Paramètre introuvable : on refuse plutôt que d'autoriser un accès non identifié.
        throw new AccesRefuseException("Identifiant de ressource introuvable pour la vérification d'accès.");
    }

    /**
     * Construit la ressource OPA, enrichie par le service si un fournisseur est disponible.
     */
    private EntreeOpa.Ressource construireRessource(String type, String id) {
        FournisseurAttributsRessource enrichisseur = fournisseur.getIfAvailable();
        if (enrichisseur != null) {
            EntreeOpa.Ressource enrichie = enrichisseur.attributs(type, id);
            if (enrichie != null) {
                return enrichie;
            }
        }
        // Ressource minimale : OPA tranche selon ses propres données (visibilité/propriétaire).
        return new EntreeOpa.Ressource(type, id, null, List.of());
    }

    /**
     * Reconstitue le contexte HTTP (méthode + chemin) si l'appel provient d'une requête web.
     */
    private EntreeOpa.Requete requeteCourante() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs) {
            HttpServletRequest requete = attributs.getRequest();
            return new EntreeOpa.Requete(requete.getMethod(), requete.getRequestURI());
        }
        // Hors contexte web (ex : appel interne) : pas de requête HTTP à transmettre.
        return null;
    }
}
