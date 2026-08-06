#!/usr/bin/env bash
set -e

echo "=== Initializing Full-Stack Repository Setup ==="

echo "1. Checking Docker & Infrastructure..."
if command -v docker &> /dev/null; then
    echo "Starting PostgreSQL via Docker Compose..."
    (cd infrastructure && docker-compose up -d)
else
    echo "Docker not found. Skipping local postgres container launch."
fi

echo "2. Installing Frontend Dependencies..."
(cd ../frontend && npm install)

echo "=== Setup Completed Successfully ==="
