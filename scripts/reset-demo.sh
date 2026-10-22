#!/usr/bin/env bash
# Borra la base y la vuelve a crear con el caso de prueba. Úsalo antes de cada ensayo de la demo.
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose down -v
APP_SEED_ENABLED=true docker compose up -d --build
echo "Demo reiniciada. API en http://localhost:8080/api/health"
