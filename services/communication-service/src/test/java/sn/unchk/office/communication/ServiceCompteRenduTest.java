package sn.unchk.office.communication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.messaging.Topics;
import sn.unchk.office.communication.domain.CompteRendu;
import sn.unchk.office.communication.domain.MeetingType;
import sn.unchk.office.communication.dto.CompteRenduCreationRequest;
import sn.unchk.office.communication.dto.CompteRenduDto;
import sn.unchk.office.communication.messaging.producer.EnregistreurEvenement;
import sn.unchk.office.communication.repository.CompteRenduRepository;
import sn.unchk.office.communication.repository.PeopleStaffRoRepository;
import sn.unchk.office.communication.service.RessourceIntrouvableException;
import sn.unchk.office.communication.service.ServiceCompteRendu;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du service des comptes rendus.
 * Vérifie la rédaction (brouillon) et la publication (émission de l'événement déclencheur).
 */
@ExtendWith(MockitoExtension.class)
class ServiceCompteRenduTest {

    @Mock
    private CompteRenduRepository compteRenduRepository;
    @Mock
    private PeopleStaffRoRepository staffRo;
    @Mock
    private EnregistreurEvenement enregistreur;

    @InjectMocks
    private ServiceCompteRendu service;

    @Test
    void rediger_cree_un_brouillon_et_emet_un_evenement_redige() {
        // Étant donné une requête de rédaction valide
        UUID createur = UUID.randomUUID();
        CompteRenduCreationRequest requete = new CompteRenduCreationRequest(
                null, "Conseil de mai", MeetingType.conseil_universite, "Contenu",
                null, LocalDate.now(), UUID.randomUUID(), Set.of("enseignant", "administratif"));
        when(compteRenduRepository.save(any(CompteRendu.class))).thenAnswer(i -> {
            CompteRendu crSauvegarde = i.getArgument(0);
            if (crSauvegarde.getId() == null) crSauvegarde.setId(UUID.randomUUID());
            return crSauvegarde;
        });
        when(staffRo.findById(any())).thenReturn(Optional.empty());

        // Quand on rédige
        CompteRenduDto dto = service.rediger(requete, createur);

        // Alors le compte rendu est en brouillon et un événement "CompteRenduRedige" est mis en file
        assertThat(dto.published()).isFalse();
        assertThat(dto.visibility()).contains("enseignant", "administratif");
        verify(enregistreur).enregistrer(eq("CompteRendu"), any(UUID.class),
                eq(Topics.COMMUNICATION_COMPTESRENDUS), eq("CompteRenduRedige"), any());
    }

    @Test
    void publier_marque_publie_et_emet_evenement_publie() {
        // Étant donné un compte rendu existant non publié
        UUID id = UUID.randomUUID();
        CompteRendu cr = new CompteRendu();
        cr.setId(id);
        cr.setTitle("CR");
        cr.setType(MeetingType.reunion);
        cr.setMeetingDate(LocalDate.now());
        cr.setAuthorId(UUID.randomUUID());
        cr.setCreatedBy(UUID.randomUUID());
        cr.setVisibility(Set.of("enseignant"));
        when(compteRenduRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(cr));
        when(compteRenduRepository.save(any(CompteRendu.class))).thenAnswer(i -> {
            CompteRendu crSauvegarde = i.getArgument(0);
            if (crSauvegarde.getId() == null) crSauvegarde.setId(UUID.randomUUID());
            return crSauvegarde;
        });
        when(staffRo.findById(any())).thenReturn(Optional.empty());

        // Quand on publie
        CompteRenduDto dto = service.publier(id);

        // Alors il est publié et l'événement déclencheur de notifications est émis
        assertThat(dto.published()).isTrue();
        assertThat(dto.publishedAt()).isNotNull();
        verify(enregistreur).enregistrer(eq("CompteRendu"), eq(id),
                eq(Topics.COMMUNICATION_COMPTESRENDUS), eq("CompteRenduPublie"), any());
    }

    @Test
    void publier_inexistant_leve_introuvable_et_n_emet_rien() {
        // Étant donné un identifiant inconnu
        UUID id = UUID.randomUUID();
        when(compteRenduRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        // Quand on publie / Alors 404 et aucun événement
        assertThatThrownBy(() -> service.publier(id))
                .isInstanceOf(RessourceIntrouvableException.class);
        verify(enregistreur, never()).enregistrer(any(), any(), any(), any(), any());
    }
}
