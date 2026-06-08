package sn.unchk.office.people.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sn.unchk.office.people.dto.EtudiantResponse;
import sn.unchk.office.people.service.RessourceIntrouvableException;
import sn.unchk.office.people.service.StudentService;
import sn.unchk.office.people.web.PeopleExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web des etudiants (tranche MVC).
 * <p>
 * On valide : le rejet 400 d'un corps invalide (Bean Validation), la traduction
 * d'une ressource introuvable en 404 (anti-enumeration), et la creation 201.
 * Les filtres de securite sont desactives ({@code addFilters = false}) : la tranche
 * cible uniquement le mapping/validation/erreurs (l'ABAC objet est teste ailleurs).
 */
@WebMvcTest(controllers = StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PeopleExceptionHandler.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService service;

    @Test
    @DisplayName("Un corps de creation invalide (INE vide) renvoie 400")
    void creation_corpsInvalide_400() throws Exception {
        // INE vide + prenom/nom manquants : la Bean Validation doit rejeter en 400.
        String corps = """
                {
                  "ine": "",
                  "firstName": "",
                  "lastName": "",
                  "gender": "femme"
                }
                """;

        mockMvc.perform(post("/api/people/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("La consultation d'un etudiant inconnu renvoie 404")
    void consultation_inconnu_404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.consulter(any(UUID.class)))
                .thenThrow(new RessourceIntrouvableException("Etudiant introuvable."));

        mockMvc.perform(get("/api/people/students/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Une creation valide renvoie 201 et la fiche creee")
    void creation_valide_201() throws Exception {
        EtudiantResponse cree = new EtudiantResponse(
                UUID.randomUUID(), "INE-2024-100", "MAT-100", "Awa", "Diop",
                sn.unchk.office.people.domain.Genre.femme, null, null, null, null, null, null,
                null, "2024-2025", (short) 2024, null, null,
                sn.unchk.office.people.domain.StudentStatus.inscrit, List.of(),
                Instant.now(), Instant.now());
        when(service.creer(any(), any())).thenReturn(cree);

        String corps = """
                {
                  "ine": "INE-2024-100",
                  "firstName": "Awa",
                  "lastName": "Diop",
                  "gender": "femme"
                }
                """;

        mockMvc.perform(post("/api/people/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ine").value("INE-2024-100"));
    }
}
