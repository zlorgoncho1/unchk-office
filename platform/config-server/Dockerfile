# ============================================================
# Image multi-étapes du Config Server (Spring Cloud Config).
# Étape 1 : build Maven dans un conteneur (PAS de Maven local).
# Étape 2 : exécution sur une image JRE 21 légère.
# ============================================================

# --- Étape de build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# POM parent du monorepo (fournit la gestion des dépendances Spring Cloud).
COPY pom.xml ./pom.xml
# Module config-server (POM + sources).
COPY platform/config-server/pom.xml ./platform/config-server/pom.xml
COPY platform/config-server/src ./platform/config-server/src

# Compile et empaquète uniquement le module config-server (et son parent).
RUN mvn -q -B -f platform/config-server/pom.xml clean package -DskipTests

# --- Étape d'exécution ---
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

# Récupère le jar exécutable produit à l'étape de build.
COPY --from=build /workspace/platform/config-server/target/*.jar app.jar

# Le Config Server écoute sur le port 8888.
EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
