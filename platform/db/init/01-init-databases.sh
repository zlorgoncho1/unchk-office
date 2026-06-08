#!/bin/bash
# Crée une base de données dédiée par microservice.
# Exécuté une seule fois par PostgreSQL à l'initialisation du volume.
set -e

for db in identity people document communication academic insertion admin; do
  echo "==> création de la base '${db}'"
  psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" \
    -c "CREATE DATABASE ${db};"
done

echo "==> bases applicatives créées"
