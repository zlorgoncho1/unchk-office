# syntax=docker/dockerfile:1
# ============================================================
# Dockerfile multi-étapes de l'insertion-service.
# Contexte de build = RACINE du dépôt (le Dockerfile copie le POM parent,
# la librairie common puis le module du service pour compiler dans le conteneur Maven).
# Étape 1 (build)  : image Maven officielle (PAS de Maven local requis).
# Étape 2 (runtime): image JRE légère ; n'embarque que le jar.
# ============================================================

# ---- Étape 1 : construction du jar avec Maven dans le conteneur ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POM parent + librairie commune (installée d'abord pour être disponible au service).
COPY pom.xml ./pom.xml
COPY libs/common ./libs/common
COPY services/insertion-service/pom.xml ./services/insertion-service/pom.xml

# Installe la librairie commune dans le dépôt local Maven du conteneur.
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f libs/common/pom.xml -DskipTests install

# Copie des sources du service puis construction du jar exécutable.
COPY services/insertion-service/src ./services/insertion-service/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f services/insertion-service/pom.xml -DskipTests package

# ---- Étape 2 : image d'exécution minimale ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root (bonne pratique de sécurité conteneur).
RUN useradd -r -u 1001 unchk
USER unchk

# Récupère uniquement le jar produit à l'étape de build.
COPY --from=build /build/services/insertion-service/target/insertion-service-*.jar /app/insertion-service.jar

# Port applicatif de l'insertion-service.
EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/insertion-service.jar"]
