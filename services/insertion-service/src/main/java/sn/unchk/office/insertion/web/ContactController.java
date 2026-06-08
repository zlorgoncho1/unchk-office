package sn.unchk.office.insertion.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.unchk.office.common.authz.VerifieAccesObjet;
import sn.unchk.office.insertion.dto.ContactLogRequest;
import sn.unchk.office.insertion.dto.ContactLogResponse;
import sn.unchk.office.insertion.service.ContactService;

import java.util.List;
import java.util.UUID;

/**
 * API REST du registre de contact (suivi des diplômés).
 * <p>
 * Chemins sous {@code /api/insertion/contacts}. L'historique d'un étudiant est protégé par
 * l'ABAC anti-IDOR (un étudiant ne voit que son propre suivi : ownerId = studentRef).
 */
@RestController
@RequestMapping("/api/insertion/contacts")
public class ContactController {

    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    /** Historique de contact d'un étudiant (ABAC anti-IDOR : type « insertion » porté par l'étudiant). */
    @GetMapping("/etudiant/{studentRef}")
    @VerifieAccesObjet(type = "insertion", action = "read", idParam = "studentRef")
    public List<ContactLogResponse> historique(@PathVariable UUID studentRef) {
        return service.historique(studentRef).stream().map(ContactLogResponse::depuis).toList();
    }

    /** Enregistrement d'un contact de suivi. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactLogResponse enregistrer(@Valid @RequestBody ContactLogRequest requete) {
        return ContactLogResponse.depuis(service.enregistrer(requete));
    }
}
