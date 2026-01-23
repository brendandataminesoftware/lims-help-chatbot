#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Building UAT documentation..."
docker build -t uat-docs .

echo "Extracting PDF..."
docker create --name uat-temp uat-docs
docker cp uat-temp:/output/uat-documentation.pdf .
docker rm uat-temp

echo "Done! Output: $SCRIPT_DIR/uat-documentation.pdf"
