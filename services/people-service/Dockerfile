# syntax=docker/dockerfile:1
# ============================================================
# people-service — image multi-etapes.
# Contexte de build = RACINE du depot (pour acceder a pom.xml + libs/common).
#   docker build -f services/people-service/Dockerfile -t people-service .
# ============================================================

# --- Etape 1 : compilation (image Maven + JDK 21) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POM parent + librairie commune (installee en premier pour le cache de couches).
COPY pom.xml ./pom.xml
COPY libs/common ./libs/common
COPY services/people-service/pom.xml ./services/people-service/pom.xml

# Construit et installe la librairie commune dans le depot local Maven.
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -N -f pom.xml install
# Installe le POM parent dans le .m2 pour resoudre les modules dependants.
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f libs/common/pom.xml -DskipTests install

# Code du service, puis package du jar executable.
COPY services/people-service/src ./services/people-service/src
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f services/people-service/pom.xml -DskipTests package

# --- Etape 2 : execution (JRE seul, image legere) ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Recupere le jar produit a l'etape de build.
COPY --from=build /build/services/people-service/target/people-service-*.jar /app/people-service.jar

# Port interne du service sur le reseau Docker.
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/people-service.jar"]
