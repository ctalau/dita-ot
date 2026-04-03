# Build cache correctness overview

This document describes the cache-correctness experiment under `experiments/exp-002-build-cache-correctness/`.

## Objective
Ensure that copying and reusing `.dita-cache/` is behaviorally equivalent to a direct build of modified sources.

## Test model
For each fixture:
1. Thread A builds `initial` with cache enabled.
2. Thread A builds `modified` with the same cache.
3. Thread B builds `modified` directly with an empty `.dita-cache/`.
4. Compare output trees by deterministic directory hashing.

A fixture passes only if outputs are byte-identical.

## Metrics
The runner prints:
- Cached-run wall time (thread A, modified build)
- Direct-run wall time (thread B, modified build)
- Per-fixture speedup (`direct / cached`)
- Overall pass/fail summary across fixtures
