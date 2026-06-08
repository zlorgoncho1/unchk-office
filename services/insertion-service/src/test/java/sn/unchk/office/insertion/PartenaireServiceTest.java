package sn.unchk.office.insertion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.insertion.domain.Partner;
import sn.unchk.office.insertion.domain.PartnerKind;
import sn.unchk.office.insertion.dto.PartnerRequest;
import sn.unchk.office.insertion.messaging.EvenementInsertion;
import sn.unchk.office.insertion.messaging.ProducteurInsertion;
import sn.unchk.office.insertion.repository.PartnerRepository;
import sn.unchk.office.insertion.service.PartenaireService;
import sn.unchk.office.insertion.web.RessourceIntrouvableException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du service des partenaires.
 * On vérifie qu'une création persiste l'entité ET publie un événement Kafka
 * (communication 100% Kafka), et qu'une consultation absente lève un 404.
 */
@ExtendWith(MockitoExtension.class)
class PartenaireServiceTest {

    @Mock
    private PartnerRepository depot;

    @Mock
    private ProducteurInsertion producteur;

    @Mock
    private AuditLogger audit;

    @InjectMocks
    private PartenaireService service;

    @Captor
    private ArgumentCaptor<Object> capturePayload;

    @Test
    void doitCreerEtPublierUnEvenement() {
        PartnerRequest requete = new PartnerRequest(
                "TechCorp", PartnerKind.entreprise, "Numérique",
                "M. Sow", "contact@techcorp.sn", "770000000",
                "Dakar Plateau", "Dakar", true);

        // Le dépôt renvoie l'entité enregistrée (avec un id généré).
        when(depot.save(any(Partner.class))).thenAnswer(invocation -> {
            Partner p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });

        Partner cree = service.creer(requete);

        // L'entité est bien enregistrée avec les valeurs du DTO.
        assertThat(cree.getName()).isEqualTo("TechCorp");
        assertThat(cree.getKind()).isEqualTo(PartnerKind.entreprise);

        // Un événement « PartenaireCree » est publié, clé = id du partenaire.
        verify(producteur).publier(eq(cree.getId().toString()),
                eq(EvenementInsertion.PARTENAIRE_CREE), capturePayload.capture());
        assertThat(capturePayload.getValue()).isNotNull();
    }

    @Test
    void doitLeverIntrouvableSiPartenaireAbsent() {
        UUID id = UUID.randomUUID();
        when(depot.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        // Anti-IDOR : ressource absente => 404 indistinct.
        assertThatThrownBy(() -> service.consulter(id))
                .isInstanceOf(RessourceIntrouvableException.class);
    }
}
