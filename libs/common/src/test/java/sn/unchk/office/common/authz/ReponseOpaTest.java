package sn.unchk.office.common.authz;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la réponse OPA {@link ReponseOpa}.
 * Vérifie la désérialisation du corps {@code {"result": ...}} et le deny-by-default.
 */
class ReponseOpaTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void result_true_est_autorise() throws Exception {
        // Quand OPA renvoie result=true
        ReponseOpa reponse = mapper.readValue("{\"result\": true}", ReponseOpa.class);
        // Alors l'accès est autorisé
        assertThat(reponse.estAutorise()).isTrue();
    }

    @Test
    void result_false_est_refuse() throws Exception {
        // Quand OPA renvoie result=false
        ReponseOpa reponse = mapper.readValue("{\"result\": false}", ReponseOpa.class);
        // Alors l'accès est refusé
        assertThat(reponse.estAutorise()).isFalse();
    }

    @Test
    void result_absent_est_refuse_par_defaut() throws Exception {
        // Quand la règle n'est pas définie (corps vide), result est null
        ReponseOpa reponse = mapper.readValue("{}", ReponseOpa.class);
        // Alors on refuse par défaut (deny-by-default)
        assertThat(reponse.result()).isNull();
        assertThat(reponse.estAutorise()).isFalse();
    }
}
