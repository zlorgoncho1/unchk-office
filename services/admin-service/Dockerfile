# ============================================================
# Dockerfile multi-étapes du service Administration (admin-service).
# Contexte de build = RACINE du dépôt (le Dockerfile copie le POM parent,
# la librairie common puis le module admin-service).
#
# Étape 1 (build)   : image Maven officielle (PAS de Maven local requis).
# Étape 2 (runtime) : image JRE légère ; n'embarque que le jar.
# ============================================================

# ---- Étape 1 : construction du jar avec Maven dans le conteneur ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POM parent (contexte = racine), puis librairie commune et POM du module.
COPY pom.xml ./pom.xml
COPY libs/common ./libs/common
COPY services/admin-service/pom.xml ./services/admin-service/pom.xml

# La librairie commune est une dépendance : on l'installe d'abord dans le dépôt local.
RUN mvn -q -f libs/common/pom.xml -DskipTests install

# Copie des sources du service puis construction du jar exécutable.
COPY services/admin-service/src ./services/admin-service/src
RUN mvn -q -f services/admin-service/pom.xml -DskipTests package

# ---- Étape 2 : image d'exécution minimale ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root (bonne pratique de sécurité conteneur).
RUN useradd -r -u 1001 unchk
USER unchk

# Récupère uniquement le jar produit à l'étape de build.
COPY --from=build /build/services/admin-service/target/admin-service-*.jar /app/admin-service.jar

# Port applicatif du service Administration.
EXPOSE 8087

ENTRYPOINT ["java", "-jar", "/app/admin-service.jar"]
