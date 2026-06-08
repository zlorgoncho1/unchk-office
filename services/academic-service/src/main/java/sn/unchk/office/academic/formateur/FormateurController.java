package sn.unchk.office.academic.formateur;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.academic.formateur.dto.AffectationCreationDto;
import sn.unchk.office.academic.formateur.dto.AffectationDto;
import sn.unchk.office.academic.formateur.dto.FormateurDto;
import sn.unchk.office.common.authz.VerifieAccesObjet;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API REST de l'affectation des formateurs, sous {@code /api/academic}.
 * <p>
 * Les noms des formateurs proviennent toujours du read-model local
 * {@code academic_formateur_ro} (alimenté par {@code people.staff}), jamais d'un appel REST.
 * Les affectations sur une formation donnée sont protégées par l'ABAC anti-IDOR sur la formation.
 */
@RestController
@RequestMapping("/api/academic")
public class FormateurController {

    private final FormateurService formateurService;

    public FormateurController(FormateurService formateurService) {
        this.formateurService = formateurService;
    }

    /**
     * Liste les formateurs connus localement (projection). Endpoint de collection (RBAC route).
     */
    @GetMapping("/formateurs")
    public List<FormateurDto> listerFormateurs() {
        return formateurService.listerFormateurs().stream().map(FormateurDto::de).toList();
    }

    /**
     * Liste les formateurs affectés à une formation, enrichis de leur nom (résolu localement).
     * Endpoint sensible : ABAC anti-IDOR sur la formation parente.
     */
    @GetMapping("/formations/{formationId}/formateurs")
    @VerifieAccesObjet(type = "formation", action = "read", idParam = "formationId")
    public List<AffectationDto> listerAffectations(@PathVariable UUID formationId) {
        List<AffectationFormateur> affectations = formateurService.listerAffectations(formationId);
        Map<UUID, String> noms = formateurService.resoudreNoms(
                affectations.stream().map(AffectationFormateur::getFormateurRef).toList());
        return affectations.stream()
                .map(a -> AffectationDto.de(a, noms.get(a.getFormateurRef())))
                .toList();
    }

    /**
     * Affecte un formateur à une formation pour un module donné.
     * Endpoint sensible : ABAC anti-IDOR sur la formation parente (action update).
     */
    @PostMapping("/formations/{formationId}/formateurs")
    @ResponseStatus(HttpStatus.CREATED)
    @VerifieAccesObjet(type = "formation", action = "update", idParam = "formationId")
    public AffectationDto affecter(@PathVariable UUID formationId,
                                   @Valid @RequestBody AffectationCreationDto dto) {
        AffectationFormateur affectation = formateurService.affecter(formationId, dto);
        String nom = formateurService.resoudreNom(affectation.getFormateurRef());
        return AffectationDto.de(affectation, nom);
    }

    /**
     * Retire l'affectation d'un formateur (par module) d'une formation.
     * Endpoint sensible : ABAC anti-IDOR sur la formation parente (action update).
     */
    @DeleteMapping("/formations/{formationId}/formateurs/{formateurRef}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @VerifieAccesObjet(type = "formation", action = "update", idParam = "formationId")
    public void retirer(@PathVariable UUID formationId,
                        @PathVariable UUID formateurRef,
                        @RequestParam("module") String module) {
        formateurService.retirerAffectation(formationId, formateurRef, module);
    }
}
