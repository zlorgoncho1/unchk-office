package sn.unchk.office.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'identity-service.
 * <p>
 * Fournisseur d'identité fédéré maison (sans Keycloak) : il gère les comptes utilisateurs
 * et leurs rôles, authentifie (BCrypt), émet des JWT RS256, expose le JWKS et publie
 * le cycle de vie des comptes sur le topic {@code identity.users}.
 */
@SpringBootApplication
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
