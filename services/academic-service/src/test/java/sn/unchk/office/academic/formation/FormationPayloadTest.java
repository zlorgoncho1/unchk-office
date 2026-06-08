package sn.unchk.office.academic.formation;

import org.junit.jupiter.api.Test;
import sn.unchk.office.academic.formation.event.FormationPayload;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de la transformation entité -> charge utile Kafka (transfert d'état).
 * On vérifie que les énumérations sont sérialisées en chaînes neutres pour les consommateurs.
 */
class FormationPayloadTest {

    @Test
    void convertit_l_entite_en_payload_avec_enums_en_chaines() {
        // Étant donné une formation renseignée...
        Formation formation = new Formation();
        formation.setId(UUID.randomUUID());
        formation.setCode("MAS-DATA");
        formation.setLabel("Master Data");
        formation.setLevel(NiveauFormation.MASTER);
        formation.setKind(TypeFormation.PROFESSIONNELLE);
        formation.setFunding(Financement.PARTENAIRE);
        formation.setStartDate(LocalDate.of(2024, 9, 1));
        formation.setTrainedMale(12);
        formation.setTrainedFemale(8);

        // Quand on construit le payload...
        FormationPayload payload = FormationPayload.de(formation);

        // Alors les énumérations sont des chaînes et les compteurs sont préservés.
        assertThat(payload.level()).isEqualTo("master");
        assertThat(payload.kind()).isEqualTo("professionnelle");
        assertThat(payload.funding()).isEqualTo("partenaire");
        assertThat(payload.trainedMale()).isEqualTo(12);
        assertThat(payload.trainedFemale()).isEqualTo(8);
        assertThat(payload.code()).isEqualTo("MAS-DATA");
    }

    @Test
    void tolere_un_financement_absent() {
        // Étant donné une formation sans financement...
        Formation formation = new Formation();
        formation.setId(UUID.randomUUID());
        formation.setLabel("Certificat");
        formation.setLevel(NiveauFormation.CERTIFICAT);

        // Quand on construit le payload, alors le financement reste null (champ optionnel).
        FormationPayload payload = FormationPayload.de(formation);
        assertThat(payload.funding()).isNull();
        assertThat(payload.level()).isEqualTo("certificat");
    }
}
