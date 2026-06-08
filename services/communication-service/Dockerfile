# syntax=docker/dockerfile:1
# ============================================================
# Dockerfile multi-étapes du communication-service.
# Contexte de build = RACINE du dépôt (pour accéder à pom.xml + libs/common).
#   docker build -f services/communication-service/Dockerfile -t communication-service .
# ============================================================

# --- Étape 1 : construction du jar avec Maven + JDK 21 ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POM parent (BOM Spring Boot + Spring Cloud) et librairie partagée.
COPY pom.xml ./pom.xml
COPY libs/common ./libs/common
# POM du service (résolution des dépendances avant de copier les sources).
COPY services/communication-service/pom.xml ./services/communication-service/pom.xml

# Installe la librairie commune dans le dépôt local Maven.
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f libs/common/pom.xml -DskipTests install

# Sources du service, puis empaquetage.
COPY services/communication-service/src ./services/communication-service/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f services/communication-service/pom.xml -DskipTests package

# --- Étape 2 : image d'exécution légère (JRE 21) ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Récupère le jar exécutable produit à l'étape de build.
COPY --from=build /build/services/communication-service/target/communication-service-*.jar /app/communication-service.jar

# Port applicatif du service.
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/communication-service.jar"]
