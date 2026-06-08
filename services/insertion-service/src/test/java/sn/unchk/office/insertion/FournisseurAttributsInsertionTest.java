package sn.unchk.office.insertion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.insertion.authz.FournisseurAttributsInsertion;
import sn.unchk.office.insertion.domain.Internship;
import sn.unchk.office.insertion.repository.InsertionOutcomeRepository;
import sn.unchk.office.insertion.repository.InternshipRepository;
import sn.unchk.office.insertion.repository.PartnerRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests du fournisseur d'attributs ABAC (anti-IDOR).
 * On vérifie que le propriétaire (ownerId) et la visibilité sont correctement renseignés
 * pour qu'OPA puisse refuser l'accès d'un étudiant au stage d'un autre.
 */
@ExtendWith(MockitoExtension.class)
class FournisseurAttributsInsertionTest {

    @Mock
    private InternshipRepository stages;

    @Mock
    private PartnerRepository partenaires;

    @Mock
    private InsertionOutcomeRepository situations;

    @InjectMocks
    private FournisseurAttributsInsertion fournisseur;

    @Test
    void doitExposerEtudiantCommeProprietaireDuStage() {
        UUID idStage = UUID.randomUUID();
        UUID idEtudiant = UUID.randomUUID();

        Internship stage = new Internship();
        stage.setId(idStage);
        stage.setStudentRef(idEtudiant);
        when(stages.findByIdAndDeletedAtIsNull(idStage)).thenReturn(Optional.of(stage));

        EntreeOpa.Ressource ressource = fournisseur.attributs("stage", idStage.toString());

        // Le propriétaire ABAC est l'étudiant : il pourra consulter SON bilan de stage.
        assertThat(ressource.ownerId()).isEqualTo(idEtudiant.toString());
        // La visibilité couvre les rôles de gestion du module.
        assertThat(ressource.visibility()).contains("appui-insertion", "admin");
    }

    @Test
    void doitRenvoyerRessourceMinimaleSiIdentifiantIllisible() {
        EntreeOpa.Ressource ressource = fournisseur.attributs("stage", "pas-un-uuid");
        // Identifiant invalide : pas de propriétaire ni de visibilité -> OPA refusera (sauf admin).
        assertThat(ressource.ownerId()).isNull();
        assertThat(ressource.visibility()).isEmpty();
    }
}
