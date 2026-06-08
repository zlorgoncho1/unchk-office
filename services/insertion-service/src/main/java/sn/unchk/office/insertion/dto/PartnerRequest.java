package sn.unchk.office.insertion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import sn.unchk.office.insertion.domain.PartnerKind;

/**
 * Données de création / mise à jour d'un partenaire (saisies client).
 * <p>
 * DTO dédié (anti sur-affectation) : les champs système (id, version, createdBy, horodatages)
 * ne sont JAMAIS liés depuis le corps client.
 *
 * @param name         nom du partenaire (obligatoire)
 * @param kind         type de partenaire (entreprise par défaut si absent)
 * @param sector       secteur d'activité
 * @param contactName  nom du contact
 * @param contactEmail courriel du contact (format vérifié)
 * @param contactPhone téléphone du contact
 * @param address      adresse
 * @param city         ville
 * @param active       partenaire actif (vrai par défaut)
 */
public record PartnerRequest(
        @NotBlank(message = "Le nom du partenaire est obligatoire.")
        @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères.")
        String name,

        PartnerKind kind,

        @Size(max = 255)
        String sector,

        @Size(max = 255)
        String contactName,

        @Email(message = "Le courriel de contact n'est pas valide.")
        @Size(max = 255)
        String contactEmail,

        @Size(max = 32, message = "Le téléphone ne doit pas dépasser 32 caractères.")
        String contactPhone,

        @Size(max = 1000)
        String address,

        @Size(max = 255)
        String city,

        Boolean active
) {
}
