# ============================================================
# Dockerfile multi-étapes de l'API Gateway.
# Étape 1 (build)  : image Maven officielle (PAS de Maven local requis).
# Étape 2 (runtime): image JRE légère ; n'embarque que le jar.
# ============================================================

# ---- Étape 1 : construction du jar avec Maven dans le conteneur ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# On copie d'abord le POM parent (contexte de build = racine du dépôt) puis les modules
# nécessaires, afin de profiter du cache de dépendances Maven.
COPY pom.xml ./pom.xml
COPY apps/api-gateway/pom.xml ./apps/api-gateway/pom.xml

# Pré-téléchargement des dépendances (cache) ; ignore l'absence éventuelle des modules frères.
RUN mvn -q -f apps/api-gateway/pom.xml -DskipTests dependency:go-offline || true

# Copie des sources puis construction du jar exécutable.
COPY apps/api-gateway/src ./apps/api-gateway/src
RUN mvn -q -f apps/api-gateway/pom.xml -DskipTests clean package

# ---- Étape 2 : image d'exécution minimale ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root (bonne pratique de sécurité conteneur).
RUN useradd -r -u 1001 unchk
USER unchk

# Récupère uniquement le jar produit à l'étape de build.
COPY --from=build /build/apps/api-gateway/target/api-gateway-*.jar /app/api-gateway.jar

# La passerelle écoute sur 8080 (port d'entrée REST/WebSocket de la plateforme).
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]
