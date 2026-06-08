package sn.unchk.office.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Corps de création d'un compte ({@code POST /api/identity/users}), réservé à l'admin.
 * <p>
 * DTO dédié (anti sur-affectation / mass assignment) : les champs système
 * ({@code id}, {@code version}, horodatages) ne sont jamais liés depuis le corps client.
 *
 * @param email      courriel unique (login)
 * @param motDePasse mot de passe initial (haché en BCrypt côté serveur)
 * @param fullName   nom complet affiché
 * @param roles      rôles à accorder (au moins un, libellés canoniques)
 * @param personRef  référence optionnelle vers une personne canonique (people)
 * @param personKind nature de la personne liée ({@code etudiant} ou {@code personnel})
 */
public record RequeteCreationUtilisateur(

        @NotBlank(message = "Le courriel est obligatoire.")
        @Email(message = "Le courriel doit être valide.")
        @Size(max = 320, message = "Le courriel est trop long.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 200, message = "Le mot de passe doit comporter entre 8 et 200 caractères.")
        String motDePasse,

        @NotBlank(message = "Le nom complet est obligatoire.")
        @Size(max = 200, message = "Le nom complet est trop long.")
        String fullName,

        @NotEmpty(message = "Au moins un rôle doit être accordé.")
        List<@Pattern(regexp = "admin|administratif|enseignant|appui-insertion|etudiant",
                message = "Rôle invalide.") String> roles,

        UUID personRef,

        @Pattern(regexp = "etudiant|personnel", message = "La nature de personne doit être 'etudiant' ou 'personnel'.")
        String personKind
) {
}
