# Cache transfer runner design notes

This experiment intentionally does **not** implement any cache-key or cache-generation logic.

## Assumption
DITA-OT build steps write reusable artifacts under `.dita-cache/`.

## What the runner does
- Builds fixture `initial` sources in thread A.
- Copies thread A `.dita-cache/` snapshot.
- Builds fixture `modified` sources in thread A after restoring copied cache.
- Builds fixture `modified` sources in thread B with a fresh/empty cache folder.
- Compares output hashes and reports timing + speedup.

## Why this model
It isolates the correctness question (cache reuse vs direct rebuild) from cache implementation details, so cache internals can evolve independently.
