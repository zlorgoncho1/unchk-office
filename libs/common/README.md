# common — librairie transverse des services métier

Module Maven partagé (`sn.unchk.office:common`) regroupant les briques **servlet / Spring MVC**
réutilisées par tous les microservices métier UNCHK Office. **Non destiné au gateway réactif**
(WebFlux) : il s'appuie sur Spring Web MVC.

Importé automatiquement via Spring Boot AutoConfiguration
(`CommonAutoConfiguration`) dès qu'un service déclare la dépendance.

## Contenu

| Paquet | Rôle |
|---|---|
| `security` | Serveur de ressources OAuth2 : validation JWT via JWKS (issuer + audience + expiration), extraction des rôles et du `userId` (claim `sub`) dans le SecurityContext. |
| `authz` | `OpaClient` (appelle `POST {OPA_URL}/v1/data/unchk/authz/allow`), annotation `@VerifieAccesObjet` + aspect `ResourceAccessGuard` pour le contrôle d'accès au niveau objet (anti-IDOR). |
| `web` | `ApiError` (erreur sans fuite), `GlobalExceptionHandler`, `CorrelationIdFilter` (`X-Correlation-Id`), `SecurityHeadersFilter`. |
| `messaging` | `Topics` (noms de topics), `DomainEvent` (enveloppe d'événement), configs producteur/consommateur Kafka (JSON). |
| `export` | `ExcelExporter` (Apache POI) et `PdfExporter` (OpenPDF). |
| `audit` | `AuditLogger` (journalisation structurée « qui a fait quoi »). |

## Configuration attendue côté service

```yaml
unchk:
  security:
    jwt:
      jwks-uri: http://identity-service:8080/.well-known/jwks.json
      issuer: unchk-office
      audience: unchk-office
  authz:
    opa:
      url: http://opa:8181        # chemin et timeout ont des valeurs par défaut
spring:
  kafka:
    bootstrap-servers: kafka:19092
    consumer:
      group-id: document-service  # propre à chaque service
```

## Anti-IDOR : exemple d'usage

```java
@VerifieAccesObjet(type = "document", action = "read", idParam = "id")
public DocumentDto consulter(@PathVariable UUID id) { ... }
```

L'aspect interroge OPA (`sujet × action × ressource`) avant de renvoyer la ressource.
Chaque service peut fournir un bean `FournisseurAttributsRessource` pour enrichir la ressource
(propriétaire, visibilité) à partir de son read-model local.
