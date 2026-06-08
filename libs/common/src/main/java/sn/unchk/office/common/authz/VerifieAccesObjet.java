package sn.unchk.office.common.authz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une méthode dont l'accès à un objet précis doit être vérifié par OPA (anti-IDOR).
 * <p>
 * Posée sur une méthode de contrôleur ou de service, elle déclenche le
 * {@link ResourceAccessGuard} qui demande à OPA l'autorisation
 * {@code sujet × action × ressource} AVANT de renvoyer la ressource.
 * <p>
 * Exemple :
 * <pre>{@code
 * @VerifieAccesObjet(type = "document", action = "read", idParam = "id")
 * public DocumentDto consulter(@PathVariable UUID id) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifieAccesObjet {

    /** Type logique de la ressource ciblée (document, formation, etudiant, ...). */
    String type();

    /** Action demandée sur la ressource (read par défaut). */
    String action() default "read";

    /**
     * Nom du paramètre de la méthode contenant l'identifiant de la ressource.
     * L'aspect lit ce paramètre pour construire la ressource envoyée à OPA.
     */
    String idParam() default "id";
}
