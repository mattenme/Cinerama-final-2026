#!/usr/bin/env bash
# Arranca Cinerama completo: backend (8082) y frontend (4200).
# Uso: ./start.sh          -> levanta los dos en segundo plano y abre el navegador
#      ./start.sh --logs   -> levanta y deja los logs en la terminal
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_BACKEND="${TMPDIR:-/tmp}/cinerama-backend.log"
LOG_FRONTEND="${TMPDIR:-/tmp}/cinerama-frontend.log"

echo "== Cinerama =="
command -v java >/dev/null || { echo "Falta Java 21 en el PATH."; exit 1; }
command -v node >/dev/null || { echo "Falta Node (18+) en el PATH."; exit 1; }

if [ ! -d "backend/target" ]; then
  echo "Aviso: el backend no está compilado todavía; la primera vez tarda un poco más."
fi

echo "-> Backend en http://localhost:8082/cinerama (log: $LOG_BACKEND)"
cd "$RAIZ/backend"
./mvnw -q -DskipTests spring-boot:run > "$LOG_BACKEND" 2>&1 &
BACKEND_PID=$!

echo "-> Frontend en http://localhost:4200 (log: $LOG_FRONTEND)"
cd "$RAIZ/frontend"
if [ ! -d node_modules ]; then
  echo "   Instalando dependencias del frontend…"
  npm install > /dev/null
fi
./node_modules/.bin/ng serve --port 4200 > "$LOG_FRONTEND" 2>&1 &
FRONTEND_PID=$!

limpiar() {
  echo
  echo "Deteniendo Cinerama…"
  kill "$FRONTEND_PID" "$BACKEND_PID" 2>/dev/null || true
  exit 0
}
trap limpiar INT TERM

echo
echo "Esperando a que el backend responda…"
for _ in $(seq 1 60); do
  if curl -fsS -o /dev/null "http://localhost:8082/cinerama/api/v1/peliculas/activas" 2>/dev/null; then
    echo "Backend listo."
    break
  fi
  sleep 2
done

echo "Frontend: http://localhost:4200"
echo "Cuentas demo: 00000001/admin123 · 11223344/empleado123 · 77889901/123456"
echo "Ctrl+C detiene los dos procesos."

if [ "${1:-}" != "--logs" ]; then
  (command -v xdg-open >/dev/null && xdg-open http://localhost:4200) \
    || (command -v open >/dev/null && open http://localhost:4200) || true
fi

wait
