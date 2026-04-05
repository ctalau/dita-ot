# HTML5 topic transform cache: structure, invalidation, and test coverage

## Why this cache exists
The HTML5 topic conversion (`DITA -> HTML`) is one of the most expensive build steps. The cache stores per-topic transform results so unchanged topics can be restored instead of re-transformed.

## Designated cache folder
By default, HTML5 topic cache is enabled via:

- `dita.cache.dir` (base cache directory)
- `dita.html5.topic.cache.enabled` (`true` by default)
- cache path for topic outputs: `${dita.cache.dir}/html5-topics`

## Cache entry structure
Each cached topic output is written as a gzip-compressed artifact:

- File suffix: `.html.gz`
- Sharded path: `<cacheDir>/<first-two-hex-chars>/<full-key>.html.gz`

This keeps entries compact on disk and avoids huge flat directories.

## Cache key
A key is SHA-256 over:

1. **Transform context hash**
   - stylesheet system ID
   - XSLT params (name + value)
   - serializer output properties
   - mapper class and extension
   - file-content hashes for file-URI params (for example filter/header/footer/map params when provided as files)
2. **Output path**
3. **Topic input bytes**
4. **Referenced local DITA/XML dependencies discovered from topic attributes**
   - `@href` and `@conref` values that resolve to local `.dita` / `.xml` files
   - fragment stripped before resolution (for example `glossentry.dita#g1` -> `glossentry.dita`)

## Invalidation policy
An entry is invalidated (new key -> miss) whenever any key component above changes.

Practical implications:

- Topic content change => invalidated.
- Output mapping/path change => invalidated.
- Relevant transform parameter change => invalidated.
- Referenced local DITA/XML dependency change (including glossary target used by `term`/`abbreviated-form` style patterns) => invalidated.
- If dependency discovery fails, the transform falls back safely (no stale serve), preferring correctness over cache hit rate.

## Required implementation rule (process)
When implementing cache logic for this step, **you must consult step documentation and step-input docs first** (especially `docs/steps/` and step dependency notes), then encode invalidation rules from those inputs. If unsure, prefer conservative invalidation over potential stale output.

## Tested cases
Top-level test coverage includes:

### Happy path
- Initial build -> modified build with one changed topic and one unchanged topic.
- Cached modified output equals direct modified output.
- Cached run reports cache hits.

### Edge cases
- Cache store/restore behavior and cache miss behavior (`TopicTransformCacheTest`).
- Missing output on store is no-op (`TopicTransformCacheTest`).
- **Glossary/abbreviation-style dependency invalidation**:
  - topic references glossary entry via `term @href`
  - glossary target changes while topic file stays same
  - cached output still matches direct output and topic entry is not reused as stale

