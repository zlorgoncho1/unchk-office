package sn.unchk.office.people.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.people.domain.Genre;
import sn.unchk.office.people.domain.Student;
import sn.unchk.office.people.domain.StudentStatus;
import sn.unchk.office.people.dto.CreerEtudiantRequest;
import sn.unchk.office.people.dto.DiplomeDto;
import sn.unchk.office.people.dto.EtudiantResponse;
import sn.unchk.office.people.messaging.producer.PeopleEventPublisher;
import sn.unchk.office.people.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du service des etudiants.
 * <p>
 * On verifie le comportement metier : creation (avec emission d'evenement),
 * rejet des doublons d'INE (conflit), absence d'evenement en cas de conflit,
 * et resolution de la fiche "me" cote serveur (anti-IDOR).
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private PeopleEventPublisher publisher;

    @Mock
    private AuditLogger audit;

    @InjectMocks
    private StudentService service;

    private CreerEtudiantRequest requeteValide;

    @BeforeEach
    void preparer() {
        // Une requete de creation coherente, reutilisee par plusieurs tests.
        requeteValide = new CreerEtudiantRequest(
                "INE-2024-001", "MAT-001", "Awa", "Diop", Genre.femme,
                LocalDate.of(2002, 1, 15), "Dakar", "awa.diop@unchk.sn", "770000000",
                "Dakar, Senegal", null, null, "2024-2025", (short) 2024, null, null,
                null, StudentStatus.inscrit,
                List.of(new DiplomeDto(null, "Baccalaureat", "secondaire", LocalDate.of(2020, 7, 1))));
    }

    @Test
    @DisplayName("La creation persiste l'etudiant et publie l'evenement people.students")
    void creation_publieEvenement() {
        // Aucun doublon : l'INE et le matricule sont libres.
        when(repository.existsByIneIgnoreCase("INE-2024-001")).thenReturn(false);
        when(repository.existsByMatricule("MAT-001")).thenReturn(false);
        // Le depot renvoie l'entite enregistree (avec son UUID genere par @PrePersist).
        when(repository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });

        UUID auteur = UUID.randomUUID();
        EtudiantResponse reponse = service.creer(requeteValide, auteur);

        // L'identite et le diplome sont bien repris.
        assertThat(reponse.id()).isNotNull();
        assertThat(reponse.ine()).isEqualTo("INE-2024-001");
        assertThat(reponse.firstName()).isEqualTo("Awa");
        assertThat(reponse.diplomas()).hasSize(1);

        // L'evenement de creation est emis exactement une fois.
        ArgumentCaptor<Student> capteur = ArgumentCaptor.forClass(Student.class);
        verify(publisher, times(1)).publierEtudiantCree(capteur.capture());
        assertThat(capteur.getValue().getIne()).isEqualTo("INE-2024-001");
    }

    @Test
    @DisplayName("Un INE deja utilise leve un conflit et n'emet aucun evenement")
    void creation_ineDuplique_conflit() {
        // L'INE existe deja en base.
        when(repository.existsByIneIgnoreCase("INE-2024-001")).thenReturn(true);

        assertThatThrownBy(() -> service.creer(requeteValide, UUID.randomUUID()))
                .isInstanceOf(ConflitDonneesException.class);

        // Aucun enregistrement, aucune publication : la transaction n'a rien produit.
        verify(repository, never()).save(any());
        verify(publisher, never()).publierEtudiantCree(any());
    }

    @Test
    @DisplayName("La fiche 'me' est resolue par le compte (user_ref), jamais par un id client")
    void ficheMe_resolueParCompte() {
        UUID compte = UUID.randomUUID();
        Student etudiant = new Student();
        etudiant.setId(UUID.randomUUID());
        etudiant.setIne("INE-2024-009");
        etudiant.setFirstName("Modou");
        etudiant.setLastName("Fall");
        etudiant.setGender(Genre.homme);
        etudiant.setStatus(StudentStatus.inscrit);
        etudiant.setUserRef(compte);
        when(repository.findActifByUserRef(compte)).thenReturn(Optional.of(etudiant));

        EtudiantResponse reponse = service.consulterFicheCompte(compte);

        assertThat(reponse.ine()).isEqualTo("INE-2024-009");
        // On s'assure que la resolution passe bien par le compte et non par un id arbitraire.
        verify(repository, times(1)).findActifByUserRef(compte);
    }

    @Test
    @DisplayName("Sans fiche liee au compte, la resolution 'me' renvoie une 404 metier")
    void ficheMe_absente_introuvable() {
        UUID compte = UUID.randomUUID();
        when(repository.findActifByUserRef(compte)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consulterFicheCompte(compte))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    @DisplayName("La suppression est logique (deletedAt) et publie un tombstone")
    void suppression_logiqueEtTombstone() {
        UUID id = UUID.randomUUID();
        Student etudiant = new Student();
        etudiant.setId(id);
        etudiant.setIne("INE-2024-010");
        etudiant.setFirstName("Bintou");
        etudiant.setLastName("Sow");
        etudiant.setGender(Genre.femme);
        etudiant.setStatus(StudentStatus.inscrit);
        when(repository.findActifById(id)).thenReturn(Optional.of(etudiant));

        UUID auteur = UUID.randomUUID();
        service.supprimer(id, auteur);

        // La suppression est logique : deletedAt est renseigne, l'entite n'est pas effacee.
        assertThat(etudiant.getDeletedAt()).isNotNull();
        verify(repository, times(1)).save(etudiant);
        verify(publisher, times(1)).publierEtudiantSupprime(id, auteur);
    }
}
