#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
FIXTURES_DIR="$ROOT_DIR/experiments/exp-002-build-cache-correctness/fixtures"
DITA_BIN="${DITA_BIN:-$ROOT_DIR/src/main/bin/dita}"
INPUT_FILE="${INPUT_FILE:-main.ditamap}"
FORMAT="${FORMAT:-html5}"
KEEP_WORKDIRS="${KEEP_WORKDIRS:-0}"
RUN_DITA_LAST_OUTPUT=""

now_ms() {
  python3 - <<'PY'
import time
print(int(time.perf_counter() * 1000))
PY
}

run_dita_build() {
  local workspace="$1"
  local output_dir="$2"
  local output

  mkdir -p "$output_dir"
  output="$(
    (
      cd "$workspace"
      "$DITA_BIN" \
        --input "$workspace/src/$INPUT_FILE" \
        --format "$FORMAT" \
        --output "$output_dir" \
        --clean.temp=yes
    ) 2>&1
  )" || {
    RUN_DITA_LAST_OUTPUT="$output"
    echo "$output" >&2
    return 1
  }

  if [[ "$output" == *"Error: Build failed with an exception"* ]]; then
    echo "$output" >&2
    return 1
  fi

  RUN_DITA_LAST_OUTPUT="$output"
}

copy_cache() {
  local from_workspace="$1"
  local to_workspace="$2"

  rm -rf "$to_workspace/.dita-cache"
  if [[ -d "$from_workspace/.dita-cache" ]]; then
    cp -R "$from_workspace/.dita-cache" "$to_workspace/.dita-cache"
  else
    mkdir -p "$to_workspace/.dita-cache"
  fi
}

hash_dir() {
  local dir="$1"
  (
    cd "$dir"
    find . -type f -print0 \
      | sort -z \
      | xargs -0 sha256sum \
      | sha256sum \
      | awk '{print $1}'
  )
}

run_fixture() {
  local fixture_dir="$1"
  local fixture_name
  fixture_name="$(basename "$fixture_dir")"

  local workdir
  workdir="$(mktemp -d "${TMPDIR:-/tmp}/cache-correctness-${fixture_name}-XXXX")"

  local thread_a="$workdir/thread-a"
  local thread_b="$workdir/thread-b"
  local shared_cache_copy="$workdir/cache-from-initial"

  mkdir -p "$thread_a" "$thread_b"

  cp -R "$fixture_dir/initial" "$thread_a/src"
  mkdir -p "$thread_a/.dita-cache"

  run_dita_build "$thread_a" "$thread_a/out-initial"

  rm -rf "$shared_cache_copy"
  if [[ -d "$thread_a/.dita-cache" ]]; then
    cp -R "$thread_a/.dita-cache" "$shared_cache_copy"
  else
    mkdir -p "$shared_cache_copy"
  fi

  rm -rf "$thread_a/src"
  cp -R "$fixture_dir/modified" "$thread_a/src"
  rm -rf "$thread_a/.dita-cache"
  cp -R "$shared_cache_copy" "$thread_a/.dita-cache"

  local start_cached end_cached cached_ms
  start_cached="$(now_ms)"
  run_dita_build "$thread_a" "$thread_a/out-modified"
  local cached_log="$RUN_DITA_LAST_OUTPUT"
  end_cached="$(now_ms)"
  cached_ms=$((end_cached - start_cached))

  cp -R "$fixture_dir/modified" "$thread_b/src"
  rm -rf "$thread_b/.dita-cache"
  mkdir -p "$thread_b/.dita-cache"

  local start_direct end_direct direct_ms
  start_direct="$(now_ms)"
  run_dita_build "$thread_b" "$thread_b/out-modified"
  end_direct="$(now_ms)"
  direct_ms=$((end_direct - start_direct))

  local hash_a hash_b
  hash_a="$(hash_dir "$thread_a/out-modified")"
  hash_b="$(hash_dir "$thread_b/out-modified")"

  local status="PASS"
  if [[ "$hash_a" != "$hash_b" ]]; then
    status="FAIL"
  fi

  local expect_cache_hit_min_file="$fixture_dir/expect-cache-hit-min.txt"
  if [[ -f "$expect_cache_hit_min_file" ]]; then
    local expected_hits actual_hits
    expected_hits="$(tr -d '[:space:]' < "$expect_cache_hit_min_file")"
    actual_hits="$(printf '%s\n' "$cached_log" | grep -c "Cache hit for " || true)"
    if [[ "$actual_hits" -lt "$expected_hits" ]]; then
      status="FAIL"
      echo "Expected at least $expected_hits cache hits for fixture '$fixture_name', saw $actual_hits" >&2
    fi
  fi

  local speedup="n/a"
  if [[ "$cached_ms" -gt 0 ]]; then
    speedup="$(python3 - <<PY
cached_ms = $cached_ms
direct_ms = $direct_ms
print(f"{direct_ms / cached_ms:.2f}x")
PY
)"
  fi

  echo "[$status] $fixture_name: cached=${cached_ms}ms direct=${direct_ms}ms speedup=$speedup"

  if [[ "$KEEP_WORKDIRS" == "1" ]]; then
    local keep_path="$ROOT_DIR/experiments/exp-002-build-cache-correctness/output/$fixture_name"
    rm -rf "$keep_path"
    mkdir -p "$(dirname "$keep_path")"
    cp -R "$workdir" "$keep_path"
  fi

  if [[ "$status" == "FAIL" ]]; then
    echo "Output mismatch for fixture '$fixture_name'" >&2
    return 1
  fi

  rm -rf "$workdir"
}

main() {
  local failures=0
  local total=0

  while IFS= read -r fixture; do
    total=$((total + 1))
    if ! run_fixture "$fixture"; then
      failures=$((failures + 1))
    fi
  done < <(find "$FIXTURES_DIR" -mindepth 1 -maxdepth 1 -type d | sort)

  if [[ "$total" -eq 0 ]]; then
    echo "No fixtures found under $FIXTURES_DIR" >&2
    return 1
  fi

  if [[ "$failures" -gt 0 ]]; then
    echo "$failures/$total fixtures failed"
    return 1
  fi

  echo "All $total fixtures passed"
}

main "$@"
