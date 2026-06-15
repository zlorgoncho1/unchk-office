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
import sn.unchk.office.admin.domain.AdminDocKind;
import sn.unchk.office.admin.dto.CommuniqueDto;
import sn.unchk.office.admin.dto.CreationCommuniqueDto;
import sn.unchk.office.admin.dto.MajCommuniqueDto;
import sn.unchk.office.admin.service.CommuniqueService;
import sn.unchk.office.common.authz.VerifieAccesObjet;

import java.util.List;
import java.util.UUID;

/**
 * API REST des communiqués administratifs (sous {@code /api/admin/communiques}).
 * <p>
 * Le RBAC grossier (rôle × route) est appliqué au gateway (admin / administratif). Sur les accès
 * à un communiqué identifié par UUID, {@link VerifieAccesObjet} déclenche l'ABAC anti-IDOR (OPA).
 * La publication déclenche les notifications automatiques aux rôles ciblés.
 */
@RestController
@RequestMapping("/api/admin/communiques")
public class CommuniqueController {

    private final CommuniqueService service;

    public CommuniqueController(CommuniqueService service) {
        this.service = service;
    }

    /** Crée un communiqué (brouillon). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommuniqueDto creer(@Valid @RequestBody CreationCommuniqueDto dto) {
        return service.creer(dto);
    }

    /** Liste les communiqués, filtrés optionnellement par nature ({@code ?kind=note_service|circulaire}). */
    @GetMapping
    public List<CommuniqueDto> lister(@RequestParam(name = "kind", required = false) AdminDocKind kind) {
        return service.lister(kind);
    }

    /** Consulte un communiqué précis (anti-IDOR : ABAC OPA sur l'objet). */
    @GetMapping("/{id}")
    @VerifieAccesObjet(type = "communique", action = "read", idParam = "id")
    public CommuniqueDto consulter(@PathVariable UUID id) {
        return service.consulter(id);
    }

    /** Met à jour un communiqué (ABAC update). */
    @PutMapping("/{id}")
    @VerifieAccesObjet(type = "communique", action = "update", idParam = "id")
    public CommuniqueDto mettreAJour(@PathVariable UUID id, @Valid @RequestBody MajCommuniqueDto dto) {
        return service.mettreAJour(id, dto);
    }

    /** Publie un communiqué → notifications automatiques aux rôles ciblés (ABAC update). */
    @PatchMapping("/{id}/publication")
    @VerifieAccesObjet(type = "communique", action = "update", idParam = "id")
    public CommuniqueDto publier(@PathVariable UUID id) {
        return service.publier(id);
    }

    /** Supprime (logiquement) un communiqué (ABAC delete). */
    @DeleteMapping("/{id}")
    @VerifieAccesObjet(type = "communique", action = "delete", idParam = "id")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
