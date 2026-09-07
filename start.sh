#!/usr/bin/env bash
set -e
SERVER_PORT="${SERVER_PORT:-29976}"
set -a
[ -f .env_842c7de8-cd98-4525-9429-c0943e421fc9 ] && . ./.env_842c7de8-cd98-4525-9429-c0943e421fc9
set +a
./gradlew bootJar -q
java -jar build/libs/*.jar --server.port=$SERVER_PORT
