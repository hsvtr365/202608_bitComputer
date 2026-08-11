#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEB_ROOT="${WEB_ROOT:-/var/www/bitcomputer/bitComProject}"
APP_NAME="${APP_NAME:-bitcomputer-api}"
APP_PORT="${APP_PORT:-8081}"

cd "$ROOT_DIR"
test -f .env || { echo '.env missing'; exit 1; }
command -v pm2 >/dev/null || { echo 'pm2 missing'; exit 1; }

git pull --ff-only

set -a
. <(sed 's/\r$//' .env)
set +a

export VITE_BASE_PATH="${VITE_BASE_PATH:-/bitComProject/}"
(cd frontend && npm ci && npm run build)

sudo install -d -m 755 "$WEB_ROOT"
sudo rsync -a frontend/dist/ "$WEB_ROOT/"

(cd backend && bash ./gradlew bootJar --no-daemon)
JAR_FILE="$(find "$ROOT_DIR/backend/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*plain.jar' -print -quit)"
test -n "$JAR_FILE" || { echo 'boot jar missing'; exit 1; }

if pm2 describe "$APP_NAME" >/dev/null 2>&1; then
  pm2 restart "$APP_NAME" --update-env
else
  pm2 start java --name "$APP_NAME" --cwd "$ROOT_DIR/backend" -- -jar "$JAR_FILE" --server.port="$APP_PORT"
fi

for _ in {1..30}; do
  curl -fsS --max-time 2 "http://127.0.0.1:${APP_PORT}/api/auth/csrf" >/dev/null && break
  sleep 1
done
curl -fsS --max-time 2 "http://127.0.0.1:${APP_PORT}/api/auth/csrf" >/dev/null
pm2 save
pm2 status "$APP_NAME"
