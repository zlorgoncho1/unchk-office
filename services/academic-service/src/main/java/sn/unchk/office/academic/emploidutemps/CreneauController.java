package sn.unchk.office.academic.emploidutemps;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.academic.emploidutemps.dto.CreneauCreationDto;
import sn.unchk.office.academic.emploidutemps.dto.CreneauDto;
import sn.unchk.office.academic.formateur.FormateurService;
import sn.unchk.office.common.authz.VerifieAccesObjet;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST des emplois du temps (créneaux), sous {@code /api/academic}.
 * <p>
 * Deux familles d'accès :
 * <ul>
 *   <li>la gestion des créneaux d'une formation ({@code /formations/{id}/creneaux}), protégée
 *       par l'ABAC anti-IDOR sur la formation ;</li>
 *   <li>la consultation d'un emploi du temps ({@code /emplois-du-temps/{formationId}}), chemin
 *       aligné sur le RBAC de l'étudiant (cf. {@code data.json} :
 *       {@code GET /api/academic/emplois-du-temps/**}).</li>
 * </ul>
 * Les noms des intervenants sont résolus depuis le read-model local, sans appel REST.
 */
@RestController
@RequestMapping("/api/academic")
public class CreneauController {

    private final CreneauService creneauService;
    private final FormateurService formateurService;

    public CreneauController(CreneauService creneauService, FormateurService formateurService) {
        this.creneauService = creneauService;
        this.formateurService = formateurService;
    }

    /**
     * Consulte l'emploi du temps d'une formation (vue lecture, accessible aux étudiants).
     * Endpoint sensible : ABAC anti-IDOR sur la formation.
     */
    @GetMapping("/emplois-du-temps/{formationId}")
    @VerifieAccesObjet(type = "formation", action = "read", idParam = "formationId")
    public List<CreneauDto> consulterEmploiDuTemps(@PathVariable UUID formationId) {
        return enrichir(creneauService.listerParFormation(formationId));
    }

    /**
     * Liste les créneaux d'une formation (vue gestion). Endpoint sensible : ABAC sur la formation.
     */
    @GetMapping("/formations/{formationId}/creneaux")
    @VerifieAccesObjet(type = "formation", action = "read", idParam = "formationId")
    public List<CreneauDto> listerCreneaux(@PathVariable UUID formationId) {
        return enrichir(creneauService.listerParFormation(formationId));
    }

    /**
     * Ajoute un créneau à l'emploi du temps d'une formation.
     * Endpoint sensible : ABAC anti-IDOR sur la formation (action update).
     */
    @PostMapping("/formations/{formationId}/creneaux")
    @ResponseStatus(HttpStatus.CREATED)
    @VerifieAccesObjet(type = "formation", action = "update", idParam = "formationId")
    public CreneauDto ajouter(@PathVariable UUID formationId,
                              @Valid @RequestBody CreneauCreationDto dto) {
        Creneau creneau = creneauService.ajouter(formationId, dto);
        return CreneauDto.de(creneau, formateurService.resoudreNom(creneau.getFormateurRef()));
    }

    /**
     * Supprime un créneau de l'emploi du temps d'une formation.
     * Endpoint sensible : ABAC anti-IDOR sur la formation (action update).
     */
    @DeleteMapping("/formations/{formationId}/creneaux/{creneauId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @VerifieAccesObjet(type = "formation", action = "update", idParam = "formationId")
    public void supprimer(@PathVariable UUID formationId, @PathVariable UUID creneauId) {
        creneauService.supprimer(formationId, creneauId);
    }

    /** Enrichit une liste de créneaux du nom de leur intervenant (résolu en une passe, localement). */
    private List<CreneauDto> enrichir(List<Creneau> creneaux) {
        Map<UUID, String> noms = formateurService.resoudreNoms(
                creneaux.stream().map(Creneau::getFormateurRef).filter(java.util.Objects::nonNull).toList());
        return creneaux.stream()
                .map(c -> CreneauDto.de(c, noms.get(c.getFormateurRef())))
                .toList();
    }
}
