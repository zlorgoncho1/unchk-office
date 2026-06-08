package sn.unchk.office.academic.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.academic.formation.Formation;
import sn.unchk.office.academic.formation.FormationRepository;
import sn.unchk.office.common.authz.EntreeOpa;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests du fournisseur d'attributs ABAC (anti-IDOR) pour les formations.
 * On vérifie que la ressource envoyée à OPA porte le bon propriétaire et la bonne visibilité,
 * et qu'une formation inexistante donne une ressource « vide » (refus / anti-énumération).
 */
@ExtendWith(MockitoExtension.class)
class FournisseurAttributsFormationTest {

    @Mock
    private FormationRepository formationRepository;

    @InjectMocks
    private FournisseurAttributsFormation fournisseur;

    @Test
    void renvoie_le_responsable_comme_proprietaire() {
        // Étant donné une formation avec un responsable...
        UUID id = UUID.randomUUID();
        UUID responsable = UUID.randomUUID();
        Formation formation = new Formation();
        formation.setId(id);
        formation.setResponsibleRef(responsable);
        when(formationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(formation));

        // Quand on construit les attributs ABAC...
        EntreeOpa.Ressource ressource = fournisseur.attributs("formation", id.toString());

        // Alors le propriétaire est le responsable et la visibilité couvre les rôles attendus.
        assertThat(ressource.ownerId()).isEqualTo(responsable.toString());
        assertThat(ressource.visibility()).contains("enseignant", "admin");
    }

    @Test
    void retombe_sur_le_createur_si_pas_de_responsable() {
        // Étant donné une formation sans responsable mais avec un créateur...
        UUID id = UUID.randomUUID();
        UUID createur = UUID.randomUUID();
        Formation formation = new Formation();
        formation.setId(id);
        formation.setCreatedBy(createur);
        when(formationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(formation));

        // Quand on construit les attributs, alors le propriétaire est le créateur.
        EntreeOpa.Ressource ressource = fournisseur.attributs("formation", id.toString());
        assertThat(ressource.ownerId()).isEqualTo(createur.toString());
    }

    @Test
    void formation_inexistante_donne_ressource_vide() {
        // Étant donné un identifiant inconnu...
        UUID id = UUID.randomUUID();
        when(formationRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        // Quand on construit les attributs, alors aucun propriétaire ni visibilité (OPA refusera).
        EntreeOpa.Ressource ressource = fournisseur.attributs("formation", id.toString());
        assertThat(ressource.ownerId()).isNull();
        assertThat(ressource.visibility()).isEmpty();
    }
}
