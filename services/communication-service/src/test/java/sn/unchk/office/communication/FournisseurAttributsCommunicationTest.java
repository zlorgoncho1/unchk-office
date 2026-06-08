package sn.unchk.office.communication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.communication.domain.CompteRendu;
import sn.unchk.office.communication.domain.MeetingType;
import sn.unchk.office.communication.repository.CompteRenduRepository;
import sn.unchk.office.communication.security.FournisseurAttributsCommunication;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du fournisseur d'attributs ABAC (anti-IDOR).
 * Vérifie que le propriétaire et la visibilité réels sont remontés à OPA, et que l'absence
 * de ressource produit une ressource vide (qui mènera à un refus -> 404).
 */
@ExtendWith(MockitoExtension.class)
class FournisseurAttributsCommunicationTest {

    @Mock
    private CompteRenduRepository compteRenduRepository;

    @InjectMocks
    private FournisseurAttributsCommunication fournisseur;

    @Test
    void attributs_dun_compte_rendu_existant_remonte_owner_et_visibilite() {
        // Étant donné un compte rendu en base avec propriétaire et visibilité
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        CompteRendu cr = new CompteRendu();
        cr.setId(id);
        cr.setType(MeetingType.reunion);
        cr.setMeetingDate(LocalDate.now());
        cr.setAuthorId(UUID.randomUUID());
        cr.setCreatedBy(owner);
        cr.setVisibility(Set.of("enseignant", "admin"));
        when(compteRenduRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(cr));

        // Quand on construit les attributs ABAC
        EntreeOpa.Ressource ressource = fournisseur.attributs(
                FournisseurAttributsCommunication.TYPE_COMPTE_RENDU, id.toString());

        // Alors OPA reçoit le propriétaire et la visibilité réels
        assertThat(ressource.ownerId()).isEqualTo(owner.toString());
        assertThat(ressource.visibility()).containsExactlyInAnyOrder("enseignant", "admin");
    }

    @Test
    void attributs_dun_compte_rendu_inexistant_remonte_ressource_vide() {
        // Étant donné un identifiant inconnu
        UUID id = UUID.randomUUID();
        when(compteRenduRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        // Quand on construit les attributs / Alors aucun propriétaire ni visibilité (-> refus -> 404)
        EntreeOpa.Ressource ressource = fournisseur.attributs(
                FournisseurAttributsCommunication.TYPE_COMPTE_RENDU, id.toString());

        assertThat(ressource.ownerId()).isNull();
        assertThat(ressource.visibility()).isEmpty();
    }

    @Test
    void attributs_avec_id_non_uuid_remonte_ressource_vide() {
        // Un identifiant non-UUID ne doit jamais autoriser l'accès : ressource vide
        EntreeOpa.Ressource ressource = fournisseur.attributs(
                FournisseurAttributsCommunication.TYPE_COMPTE_RENDU, "pas-un-uuid");

        assertThat(ressource.ownerId()).isNull();
        assertThat(ressource.visibility()).isEmpty();
    }
}
