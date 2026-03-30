#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
OUT_DIR="$ROOT_DIR/experiments/exp-000-baseline/output"
MAP_FILE="$ROOT_DIR/samples/oxygenxml-userguide/DITA/UserManual.ditamap"
PROP_FILE="$OUT_DIR/pipeline-timing.properties"
LOG_FILE="$OUT_DIR/timed.log"

mkdir -p "$OUT_DIR"

cat > "$PROP_FILE" <<'PROPS'
dita.pipeline.timing=true
PROPS

"$ROOT_DIR/src/main/bin/dita" \
  --input "$MAP_FILE" \
  --format html5 \
  --output "$OUT_DIR/html5-timed" \
  --propertyfile "$PROP_FILE" \
  --clean.temp=no \
  --verbose \
  --logfile "$LOG_FILE"

echo "Timed build complete."
echo "Log: $LOG_FILE"
