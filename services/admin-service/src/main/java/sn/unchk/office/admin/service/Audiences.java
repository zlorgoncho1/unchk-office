package sn.unchk.office.admin.service;

import java.util.List;
import java.util.Set;

/**
 * Correspondance entre une « audience » (préréglage de diffusion exposé à l'UI) et la liste
 * réelle des rôles destinataires stockée dans {@code communique_targets}.
 * <p>
 * Permet à l'interface d'offrir un choix simple (select unique) tout en conservant un ciblage
 * par rôles côté serveur. La conversion inverse ({@link #audiencePour}) sert au pré-remplissage
 * du formulaire d'édition.
 */
public final class Audiences {

    /** Tous les rôles de l'établissement. */
    public static final String TOUS = "tous";
    /** Personnel (hors étudiants). */
    public static final String PERSONNEL = "personnel";
    /** Corps enseignant uniquement. */
    public static final String ENSEIGNANTS = "enseignants";
    /** Étudiants uniquement. */
    public static final String ETUDIANTS = "etudiants";
    /** Administration (admin + administratif). */
    public static final String ADMINISTRATION = "administration";

    private static final List<String> R_TOUS =
            List.of("admin", "administratif", "enseignant", "appui-insertion", "etudiant");
    private static final List<String> R_PERSONNEL =
            List.of("admin", "administratif", "enseignant", "appui-insertion");
    private static final List<String> R_ENSEIGNANTS = List.of("enseignant");
    private static final List<String> R_ETUDIANTS = List.of("etudiant");
    private static final List<String> R_ADMINISTRATION = List.of("admin", "administratif");

    private Audiences() {
        // Classe utilitaire.
    }

    /** Rôles destinataires correspondant à une audience (défaut : administration). */
    public static List<String> rolesPour(String audience) {
        if (audience == null) {
            return R_ADMINISTRATION;
        }
        return switch (audience) {
            case TOUS -> R_TOUS;
            case PERSONNEL -> R_PERSONNEL;
            case ENSEIGNANTS -> R_ENSEIGNANTS;
            case ETUDIANTS -> R_ETUDIANTS;
            default -> R_ADMINISTRATION;
        };
    }

    /** Audience correspondant à un ensemble de rôles (best-effort ; « personnalise » sinon). */
    public static String audiencePour(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return ADMINISTRATION;
        }
        if (roles.containsAll(R_TOUS)) {
            return TOUS;
        }
        if (roles.size() == R_PERSONNEL.size() && roles.containsAll(R_PERSONNEL)) {
            return PERSONNEL;
        }
        if (roles.size() == 1 && roles.contains("enseignant")) {
            return ENSEIGNANTS;
        }
        if (roles.size() == 1 && roles.contains("etudiant")) {
            return ETUDIANTS;
        }
        if (roles.size() == R_ADMINISTRATION.size() && roles.containsAll(R_ADMINISTRATION)) {
            return ADMINISTRATION;
        }
        return "personnalise";
    }
}
