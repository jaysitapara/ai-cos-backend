#!/usr/bin/env bash
set -e

echo "Starting Backend and Frontend in Development Mode..."

(cd .. && ./gradlew bootRun) &
BACKEND_PID=$!

(cd ../../frontend && npm run dev) &
FRONTEND_PID=$!

trap "kill $BACKEND_PID $FRONTEND_PID" EXIT
wait
