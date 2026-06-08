package sn.unchk.office.document.authz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sn.unchk.office.common.authz.EntreeOpa;
import sn.unchk.office.document.domain.Document;
import sn.unchk.office.document.repository.DocumentRepository;
import sn.unchk.office.document.repository.DocumentVisibilityRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests du fournisseur d'attributs ABAC (anti-IDOR) : il alimente OPA avec le propriétaire
 * et la visibilité par rôle lus depuis la base locale.
 */
@ExtendWith(MockitoExtension.class)
class FournisseurAttributsDocumentTest {

    @Mock
    private DocumentRepository documents;

    @Mock
    private DocumentVisibilityRepository visibilites;

    @InjectMocks
    private FournisseurAttributsDocument fournisseur;

    @Test
    void unDocumentExistantExposeProprietaireEtVisibilite() {
        // Étant donné un document existant avec un propriétaire et des rôles visibles...
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        Document document = new Document();
        document.setId(id);
        document.setOwnerId(owner);
        when(documents.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(document));
        when(visibilites.rolesAutorises(id)).thenReturn(List.of("administratif", "enseignant"));

        // Quand OPA demande ses attributs...
        EntreeOpa.Ressource ressource = fournisseur.attributs("document", id.toString());

        // Alors ownerId et visibility[] sont renseignés pour la décision anti-IDOR.
        assertThat(ressource.type()).isEqualTo("document");
        assertThat(ressource.ownerId()).isEqualTo(owner.toString());
        assertThat(ressource.visibility()).containsExactly("administratif", "enseignant");
    }

    @Test
    void unDocumentInconnuRenvoieUneRessourceVide() {
        // Étant donné un UUID inconnu (ou supprimé)...
        UUID id = UUID.randomUUID();
        when(documents.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        // Quand OPA demande ses attributs...
        EntreeOpa.Ressource ressource = fournisseur.attributs("document", id.toString());

        // Alors aucun attribut n'est exposé : OPA refusera (deny-by-default -> 404 ensuite).
        assertThat(ressource.ownerId()).isNull();
        assertThat(ressource.visibility()).isEmpty();
    }

    @Test
    void unIdentifiantNonUuidNAutorisePas() {
        // Un identifiant mal formé ne doit jamais conduire à une autorisation.
        EntreeOpa.Ressource ressource = fournisseur.attributs("document", "pas-un-uuid");

        assertThat(ressource.ownerId()).isNull();
        assertThat(ressource.visibility()).isEmpty();
    }
}
