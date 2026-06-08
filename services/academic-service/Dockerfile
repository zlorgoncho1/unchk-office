# ============================================================
# Dockerfile multi-étapes du academic-service.
# Contexte de build = RACINE du dépôt (le compose copie le POM parent,
# la librairie common puis le module academic-service).
# Étape 1 (build)  : image Maven officielle (PAS de Maven local requis).
# Étape 2 (runtime): image JRE légère ; n'embarque que le jar.
# ============================================================

# ---- Étape 1 : construction du jar avec Maven dans le conteneur ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POM parent + librairie commune (dépendance du service).
COPY pom.xml ./pom.xml
COPY libs/common ./libs/common
COPY services/academic-service/pom.xml ./services/academic-service/pom.xml

# Installe d'abord la librairie commune dans le dépôt local Maven du conteneur.
RUN mvn -q -f libs/common/pom.xml -DskipTests install

# Pré-téléchargement des dépendances du service (cache) ; tolère l'absence de sources.
RUN mvn -q -f services/academic-service/pom.xml -DskipTests dependency:go-offline || true

# Copie des sources puis construction du jar exécutable.
COPY services/academic-service/src ./services/academic-service/src
RUN mvn -q -f services/academic-service/pom.xml -DskipTests package

# ---- Étape 2 : image d'exécution minimale ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root (bonne pratique de sécurité conteneur).
RUN useradd -r -u 1001 unchk
USER unchk

# Récupère uniquement le jar produit à l'étape de build.
COPY --from=build /build/services/academic-service/target/academic-service-*.jar /app/academic-service.jar

# Le service écoute sur 8085 dans le réseau Docker (unchk-net).
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/academic-service.jar"]
