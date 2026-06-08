# syntax=docker/dockerfile:1
# ============================================================
# document-service — image multi-étapes (contexte de build = RACINE du dépôt)
# Étape 1 : build Maven (compile common puis le service).
# Étape 2 : image d'exécution légère (JRE 21).
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POM parent + librairie commune (construite et installée dans le dépôt local Maven).
COPY pom.xml ./pom.xml
COPY libs/common ./libs/common
COPY services/document-service/pom.xml ./services/document-service/pom.xml

# Installe la librairie commune (dépendance du service).
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f libs/common/pom.xml -DskipTests install

# Sources du service puis package du jar exécutable.
COPY services/document-service/src ./services/document-service/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f services/document-service/pom.xml -DskipTests package

# ------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copie du jar Spring Boot exécutable produit à l'étape de build.
COPY --from=build /build/services/document-service/target/document-service-*.jar /app/document-service.jar

# Port HTTP du service.
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/document-service.jar"]
