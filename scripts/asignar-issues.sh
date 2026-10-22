#!/usr/bin/env bash
# Asigna los issues a cada desarrollador según su etiqueta (dev-a / dev-b / dev-c).
# Úsalo después de que los 3 hayan aceptado la invitación al repositorio.
set -euo pipefail
REPO="ManuelCaceresDuocUC/gestion-proyectos"
asignar() { # asignar etiqueta usuario
  gh issue list -R "$REPO" --label "$1" --state all --limit 300 --json number --jq '.[].number' |
    while read -r n; do gh issue edit "$n" -R "$REPO" --add-assignee "$2" >/dev/null && echo "#$n -> $2"; done
}
asignar dev-a "Manuel-dev-a"
asignar dev-b "Hector-dev-b"
asignar dev-c "Cristian-dev-c"
# Tareas de equipo: las 3 personas
for n in $(gh issue list -R "$REPO" --label equipo --state all --limit 100 --json number --jq '.[].number'); do
  gh issue edit "$n" -R "$REPO" --add-assignee "Manuel-dev-a,Hector-dev-b,Cristian-dev-c" >/dev/null && echo "#$n -> equipo"
done
