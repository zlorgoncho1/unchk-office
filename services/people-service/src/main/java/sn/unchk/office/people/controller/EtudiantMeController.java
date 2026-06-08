package sn.unchk.office.people.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.people.dto.EtudiantResponse;
import sn.unchk.office.people.service.RessourceIntrouvableException;
import sn.unchk.office.people.service.StudentService;

import java.util.UUID;

/**
 * Fiche personnelle de l'etudiant connecte, exposee sous {@code /api/etudiants/me}.
 * <p>
 * Anti-IDOR fort (cf. docs/security.md 2.2) : le role {@code etudiant} n'a en RBAC que
 * {@code GET /api/etudiants/me/**}. Il ne peut donc PAS formuler d'URL vers un autre dossier.
 * Le service resout {@code me} -> {@code subject.id} (claim {@code sub} du JWT) COTE SERVEUR,
 * jamais via un {@code id} fourni par le client. Aucune autre fiche n'est accessible par ce
 * point d'entree.
 */
@RestController
@RequestMapping("/api/etudiants")
public class EtudiantMeController {

    private final StudentService service;

    public EtudiantMeController(StudentService service) {
        this.service = service;
    }

    /**
     * Renvoie la fiche de l'etudiant lie au compte courant.
     * Le compte ({@code sub}) est resolu sur la colonne {@code user_ref} de l'etudiant.
     */
    @GetMapping("/me")
    public EtudiantResponse maFiche(@AuthenticationPrincipal Jwt jwt) {
        UUID compte = sujetCourant(jwt);
        if (compte == null) {
            // Sans sujet identifie, aucune fiche n'est resoluble : 404 sobre.
            throw new RessourceIntrouvableException("Aucune fiche etudiant pour ce compte.");
        }
        return service.consulterFicheCompte(compte);
    }

    private UUID sujetCourant(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
