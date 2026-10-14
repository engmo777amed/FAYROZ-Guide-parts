#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if command -v gradle >/dev/null 2>&1; then exec gradle "${@:-:app:assembleDebug}"; fi
VERSION=8.11.1
GRADLE_DIR=".gradle-local/gradle-${VERSION}"
if [[ ! -x "$GRADLE_DIR/bin/gradle" ]]; then
  mkdir -p .gradle-local
  curl -fL "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip" -o .gradle-local/gradle.zip
  unzip -q .gradle-local/gradle.zip -d .gradle-local
  rm .gradle-local/gradle.zip
fi
exec "${GRADLE_DIR}/bin/gradle" "${@:-:app:assembleDebug}"
