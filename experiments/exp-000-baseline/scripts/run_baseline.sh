#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
SAMPLE_DIR="$ROOT_DIR/samples/oxygenxml-userguide"
OUT_DIR="$ROOT_DIR/experiments/exp-000-baseline/output"
LOG_FILE="$OUT_DIR/baseline.log"

if [[ ! -d "$SAMPLE_DIR/.git" ]]; then
  echo "Missing sample sources. Run setup_samples.sh first." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

# Baseline transform example. Adjust map path as needed for specific userguide branches.
MAP_FILE="$SAMPLE_DIR/DITA/UserManual.ditamap"

if [[ ! -f "$MAP_FILE" ]]; then
  echo "Expected map file not found: $MAP_FILE" >&2
  exit 1
fi

cd "$ROOT_DIR"

./gradlew -q :src:main:dist

"$ROOT_DIR/src/main/bin/dita" \
  --input "$MAP_FILE" \
  --format html5 \
  --output "$OUT_DIR/html5" \
  2>&1 | tee "$LOG_FILE"

echo "Baseline completed. Output in $OUT_DIR"
