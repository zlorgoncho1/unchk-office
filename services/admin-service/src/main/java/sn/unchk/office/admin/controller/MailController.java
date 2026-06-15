package sn.unchk.office.admin.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.admin.domain.MailDirection;
import sn.unchk.office.admin.domain.MailStatus;
import sn.unchk.office.admin.dto.ChangementStatutMailDto;
import sn.unchk.office.admin.dto.CreationMailDto;
import sn.unchk.office.admin.dto.MailDto;
import sn.unchk.office.admin.dto.MajMailDto;
import sn.unchk.office.admin.service.MailService;
import sn.unchk.office.common.authz.VerifieAccesObjet;

import java.util.List;
import java.util.UUID;

/**
 * API REST du registre du courrier (sous {@code /api/admin/mails}).
 * <p>
 * Le RBAC grossier (rôle × route) est appliqué au gateway (réservé à admin / administratif).
 * Sur les accès à un courrier identifié par UUID, {@link VerifieAccesObjet} déclenche l'ABAC
 * anti-IDOR (OPA) : la garde charge propriétaire/visibilité réels et OPA tranche.
 */
@RestController
@RequestMapping("/api/admin/mails")
public class MailController {

    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    /** Enregistre un courrier (arrivé / départ). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MailDto creer(@Valid @RequestBody CreationMailDto dto) {
        return mailService.creer(dto);
    }

    /** Liste les courriers, filtrés optionnellement par sens ({@code ?direction=}) et statut ({@code ?statut=}). */
    @GetMapping
    public List<MailDto> lister(@RequestParam(name = "direction", required = false) MailDirection direction,
                                @RequestParam(name = "statut", required = false) MailStatus statut) {
        return mailService.lister(direction, statut);
    }

    /** Consulte un courrier précis (anti-IDOR : ABAC OPA sur l'objet). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "courrier", action = "read", idParam = "id")
    public MailDto consulter(@PathVariable UUID id) {
        return mailService.consulter(id);
    }

    /** Met à jour un courrier (propriétaire requis : ABAC update). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "courrier", action = "update", idParam = "id")
    public MailDto mettreAJour(@PathVariable UUID id, @Valid @RequestBody MajMailDto dto) {
        return mailService.mettreAJour(id, dto);
    }

    /** Fait évoluer le statut d'un courrier (ABAC update). */
    @PatchMapping("/{id}/statut")
    @VerifieAccesObjet(type = "courrier", action = "update", idParam = "id")
    public MailDto changerStatut(@PathVariable UUID id, @Valid @RequestBody ChangementStatutMailDto dto) {
        return mailService.changerStatut(id, dto);
    }

    /** Supprime (logiquement) un courrier (ABAC delete). */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = "courrier", action = "delete", idParam = "id")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        mailService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
