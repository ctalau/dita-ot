#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
SAMPLES_DIR="$ROOT_DIR/samples"
REPO_DIR="$SAMPLES_DIR/oxygenxml-userguide"
REPO_URL="https://github.com/oxygenxml/userguide.git"
# Pinned for reproducibility (as of 2026-03-29).
PINNED_COMMIT="22e143cfdb3fce978c9368c79a399db1f16c9852"

mkdir -p "$SAMPLES_DIR"

if [[ ! -d "$REPO_DIR/.git" ]]; then
  git clone "$REPO_URL" "$REPO_DIR"
fi

cd "$REPO_DIR"
git fetch --all --tags
git checkout "$PINNED_COMMIT"

echo "Checked out Oxygen XML userguide at: $(git rev-parse HEAD)"
