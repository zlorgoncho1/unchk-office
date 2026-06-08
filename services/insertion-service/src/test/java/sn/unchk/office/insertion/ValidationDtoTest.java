package sn.unchk.office.insertion;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sn.unchk.office.insertion.domain.InsertionKind;
import sn.unchk.office.insertion.dto.InsertionOutcomeRequest;
import sn.unchk.office.insertion.dto.InternshipRequest;
import sn.unchk.office.insertion.dto.PartnerRequest;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la validation Bean Validation des DTO d'entrée.
 * On vérifie que les contraintes (champs obligatoires, bornes de note, courriel) sont actives.
 */
class ValidationDtoTest {

    private static ValidatorFactory factory;
    private static Validator validateur;

    @BeforeAll
    static void initialiser() {
        factory = Validation.buildDefaultValidatorFactory();
        validateur = factory.getValidator();
    }

    @AfterAll
    static void fermer() {
        factory.close();
    }

    @Test
    void partenaireSansNomEstInvalide() {
        PartnerRequest requete = new PartnerRequest(
                "   ", null, null, null, null, null, null, null, null);
        Set<ConstraintViolation<PartnerRequest>> violations = validateur.validate(requete);
        // Le nom est obligatoire (NotBlank).
        assertThat(violations).isNotEmpty();
    }

    @Test
    void partenaireAvecCourrielInvalideEstRejete() {
        PartnerRequest requete = new PartnerRequest(
                "TechCorp", null, null, null, "pas-un-email", null, null, null, null);
        Set<ConstraintViolation<PartnerRequest>> violations = validateur.validate(requete);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("contactEmail"));
    }

    @Test
    void stageSansEtudiantOuTitreEstInvalide() {
        InternshipRequest requete = new InternshipRequest(
                null, null, "", null, null, null, null, null, null, null);
        Set<ConstraintViolation<InternshipRequest>> violations = validateur.validate(requete);
        // studentRef (NotNull) et title (NotBlank) doivent être signalés.
        assertThat(violations).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void stageAvecNoteHorsBornesEstRejete() {
        InternshipRequest requete = new InternshipRequest(
                UUID.randomUUID(), null, "Stage", null, null, null, null, null, null,
                new BigDecimal("25.0"));
        Set<ConstraintViolation<InternshipRequest>> violations = validateur.validate(requete);
        // La note maximale est 20.
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("grade"));
    }

    @Test
    void situationSansTypeEstInvalide() {
        InsertionOutcomeRequest requete = new InsertionOutcomeRequest(
                UUID.randomUUID(), null, null, null, null, null, null);
        Set<ConstraintViolation<InsertionOutcomeRequest>> violations = validateur.validate(requete);
        // Le type d'insertion (kind) est obligatoire.
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("kind"));
    }

    @Test
    void situationValideNeProduitAucuneViolation() {
        InsertionOutcomeRequest requete = new InsertionOutcomeRequest(
                UUID.randomUUID(), UUID.randomUUID(), InsertionKind.auto_emploi,
                "Mon Entreprise", "Fondateur", null, true);
        Set<ConstraintViolation<InsertionOutcomeRequest>> violations = validateur.validate(requete);
        assertThat(violations).isEmpty();
    }
}
