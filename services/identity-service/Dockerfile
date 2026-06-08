# syntax=docker/dockerfile:1
# ============================================================
# Dockerfile multi-étapes de l'identity-service.
# Contexte de build = RACINE du dépôt (pour accéder à pom.xml + libs/common).
# Étape 1 (build)  : image Maven officielle (PAS de Maven local requis).
# Étape 2 (runtime): image JRE légère ; n'embarque que le jar.
# ============================================================

# ---- Étape 1 : construction du jar avec Maven dans le conteneur ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POM parent + librairie partagée + POM du service (cache des dépendances Maven).
COPY pom.xml ./pom.xml
COPY libs/common ./libs/common
COPY services/identity-service/pom.xml ./services/identity-service/pom.xml

# Installe d'abord la librairie commune dans le dépôt local Maven du conteneur.
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -N -f pom.xml install
# Installe le POM parent dans le .m2 pour resoudre les modules dependants.
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f libs/common/pom.xml -DskipTests install

# Copie les sources du service puis construit le jar exécutable.
COPY services/identity-service/src ./services/identity-service/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f services/identity-service/pom.xml -DskipTests package

# ---- Étape 2 : image d'exécution minimale ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root (bonne pratique de sécurité conteneur).
RUN useradd -r -u 1001 unchk
USER unchk

# Récupère uniquement le jar produit à l'étape de build.
COPY --from=build /build/services/identity-service/target/identity-service-*.jar /app/identity-service.jar

# Port applicatif de l'identity-service.
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/identity-service.jar"]
