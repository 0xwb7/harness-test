#!/usr/bin/env bash

set -euo pipefail

echo "================================="
echo " Spring Boot Verification"
echo "================================="

echo "[1/2] Cleaning previous build..."
./gradlew --no-daemon clean

echo "[2/2] Building and testing..."
./gradlew --no-daemon build

echo "================================="
echo " Verification PASSED"
echo "================================="
