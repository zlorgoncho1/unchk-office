package sn.unchk.office.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import sn.unchk.office.common.audit.AuditLogger;
import sn.unchk.office.document.config.MinioProprietes;
import sn.unchk.office.document.config.UploadProprietes;
import sn.unchk.office.document.domain.Document;
import sn.unchk.office.document.domain.OutboxMessage;
import sn.unchk.office.document.dto.CreerDocumentRequete;
import sn.unchk.office.document.dto.DocumentReponse;
import sn.unchk.office.document.repository.DocumentRepository;
import sn.unchk.office.document.repository.DocumentShareRepository;
import sn.unchk.office.document.repository.DocumentVisibilityRepository;
import sn.unchk.office.document.repository.OutboxRepository;
import sn.unchk.office.document.storage.StockageObjet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la logique de création de document : validation d'upload, dépôt MinIO,
 * persistance, visibilité et écriture Outbox (publication différée vers Kafka).
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documents;
    @Mock
    private DocumentVisibilityRepository visibilites;
    @Mock
    private DocumentShareRepository partages;
    @Mock
    private OutboxRepository outbox;
    @Mock
    private StockageObjet stockage;

    private DocumentService service;

    private final MinioProprietes minioProprietes =
            new MinioProprietes("http://minio:9000", "u", "p", "documents", "courriers", 300);
    private final UploadProprietes uploadProprietes =
            new UploadProprietes(26214400L, List.of("application/pdf"));

    @BeforeEach
    void preparer() {
        AuditLogger audit = new AuditLogger();
        service = new DocumentService(documents, visibilites, partages, outbox, stockage,
                minioProprietes, uploadProprietes, new ObjectMapper().findAndRegisterModules(), audit);

        // On simule un utilisateur authentifié (claim sub = UUID).
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_administratif")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creerDeposeLeBinaireEtEcritUnEvenementOutbox() {
        // Étant donné une circulaire PDF valide à déposer...
        when(documents.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MultipartFile fichier = new MockMultipartFile(
                "file", "circulaire.pdf", "application/pdf", "contenu".getBytes());
        CreerDocumentRequete requete = new CreerDocumentRequete(
                "Circulaire 2026", "circulaire", "Rentrée",
                List.of("administratif", "enseignant"), "admin-service", null);

        // Quand on crée le document...
        DocumentReponse reponse = service.creer(requete, fichier, "trace-xyz");

        // Alors : binaire déposé (bucket "documents" pour une circulaire), Outbox écrit.
        verify(stockage).deposer(org.mockito.ArgumentMatchers.eq("documents"),
                anyString(), any(), anyLong(), anyString());
        assertThat(reponse.title()).isEqualTo("Circulaire 2026");
        assertThat(reponse.category()).isEqualTo("circulaire");
        assertThat(reponse.visibility()).containsExactlyInAnyOrder("administratif", "enseignant");

        ArgumentCaptor<OutboxMessage> capteur = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).save(capteur.capture());
        assertThat(capteur.getValue().getTopic()).isEqualTo("document.documents");
        assertThat(capteur.getValue().getEventType()).isEqualTo("Created");
        assertThat(capteur.getValue().getTraceId()).isEqualTo("trace-xyz");
    }

    @Test
    void creerRefuseUnTypeMimeNonAutorise() {
        // Un type MIME hors liste blanche doit être rejeté avant tout dépôt MinIO.
        MultipartFile fichier = new MockMultipartFile(
                "file", "script.exe", "application/x-msdownload", "x".getBytes());
        CreerDocumentRequete requete = new CreerDocumentRequete(
                "Fichier piégé", "autre", null, List.of(), null, null);

        assertThatThrownBy(() -> service.creer(requete, fichier, "trace"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(stockage, never()).deposer(any(), any(), any(), anyLong(), any());
        verify(documents, never()).save(any());
    }

    @Test
    void consulterUnDocumentInconnuLeve404() {
        // Un document absent doit conduire à une 404 (anti-énumération).
        UUID id = UUID.randomUUID();
        when(documents.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consulter(id))
                .isInstanceOf(RessourceIntrouvableException.class);
    }
}
