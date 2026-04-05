# Experiment 002: Build-cache correctness harness

## Goal
Validate `.dita-cache/` reuse behavior and prove correctness against direct DITA-OT builds while reporting measured speedup.

## What worked
- Implemented a Bash correctness runner (`scripts/run_cache_correctness.sh`) that executes the required two-thread scenario for each fixture:
  - **Thread A:** build `initial` -> persist `.dita-cache` -> build `modified` using cache.
  - **Thread B:** build `modified` directly without cache reuse.
  - Compare resulting outputs and report timing speedup.
- Added optional fixture assertions for minimum cache-hit count by placing `expect-cache-hit-min.txt` in a fixture.
- Added fixture structure with `initial/` and `modified/` plus per-fixture README files.

## What didn't
- This experiment does not implement a cache algorithm; it validates behavior of the current implementation and cache transfer.
- Some runtime/environment factors can still affect byte-for-byte outputs on certain systems (for example locale-specific tooling behavior).

## What we could try further
- Add fixture-level input/format overrides so each fixture can test multiple trans-types.
- Add fixture expansion for conref/keyref-heavy projects and plugin-driven builds.
- Persist benchmark history across CI runs and show trend charts.

## Honest opinion on direction
Yes—this is the right direction for proving **correctness first** with reproducible evidence. It gives us a safe baseline before attempting more aggressive, fine-grained caching.

## Reproducible setup

```bash
./gradlew -q buildLocal
experiments/exp-002-build-cache-correctness/scripts/run_cache_correctness.sh
```

Optional: preserve working directories for manual inspection.

```bash
KEEP_WORKDIRS=1 experiments/exp-002-build-cache-correctness/scripts/run_cache_correctness.sh
```
