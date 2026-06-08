# syntax=docker/dockerfile:1
# ============================================================
# Image du frontend Angular : build (Node) puis service statique (nginx).
# ============================================================

# ---- Étape 1 : build de l'application en mode production ----
FROM node:22-alpine AS build
WORKDIR /app
ENV NG_CLI_ANALYTICS=ci

# Dépendances d'abord (cache npm) puis sources.
# npm install (et non ci) pour tolérer les deps optionnelles multi-plateformes (esbuild).
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm install --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# ---- Étape 2 : service statique nginx (SPA) ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Le builder Angular écrit les fichiers du navigateur dans dist/frontend/browser.
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
