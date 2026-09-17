#!/usr/bin/env sh
set -eu

REPOSITORY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
HEALTH_URL=http://localhost:8000/health

cd "$REPOSITORY_ROOT"

cleanup() {
  echo "Stopping the SageMath service..."
  docker compose stop sage-math
}
trap cleanup EXIT INT TERM

docker compose up --build --detach sage-math

echo "Waiting for SageMath at $HEALTH_URL ..."
attempt=1
until curl --fail --silent "$HEALTH_URL" | grep --quiet '"status":"ok"'; do
  if [ "$attempt" -ge 60 ]; then
    echo "SageMath did not become healthy within 120 seconds." >&2
    echo "Run 'docker compose logs sage-math' for details." >&2
    exit 1
  fi
  attempt=$((attempt + 1))
  sleep 2
done

echo "SageMath is ready. Starting Spring Boot..."
./mvnw spring-boot:run
