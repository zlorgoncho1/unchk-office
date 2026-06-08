package sn.unchk.office.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Point d'entrée du serveur de configuration centralisé.
 *
 * <p>Tous les microservices (gateway, identity, people, document, communication,
 * academic, insertion, admin) récupèrent leur configuration ici au démarrage.</p>
 *
 * <p>Le profil "native" est utilisé : la configuration est servie directement
 * depuis le classpath (src/main/resources/config/) plutôt que depuis un dépôt Git.
 * Cela garde l'infrastructure simple et auto-contenue dans l'image Docker.</p>
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
