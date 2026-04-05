# Cache speedups (Oxygen XML User Guide sources)

## Cache implemented
The implemented cache is a per-topic HTML5 transform output cache stored in `${dita.cache.dir}/html5-topics`.

- Entry format: gzip-compressed HTML output (`.html.gz`)
- Key: SHA-256 over transform context + output path + topic bytes + discovered local DITA/XML dependencies (`@href`, `@conref`)
- Safety-first behavior: cache I/O/dependency parsing failures fall back to normal transform.

## Measurement setup
- Toolkit build: local `buildLocal`
- Source checkout: `samples/oxygenxml-userguide` pinned by setup script
- DITA map used for benchmark:
  - `samples/oxygenxml-userguide/DITA/maps/chapter-editing-documents.ditamap`
- Command shape:
  - `src/main/bin/dita --format html5 --clean.temp=yes --propertyfile <cache-props>`
- Cache directory:
  - `experiments/exp-001-pipeline-timing/output/.dita-cache-bench`

## Results
- Cold build: **270s**
- Warm build (same cache): **153s**
- Time saved: **117s**
- Relative speedup: **1.76x** (`270 / 153`)
- Cache size on disk after warm run: **36M**

## Note
A full build of `DITA/UserManual.ditamap` in this pinned checkout currently fails due missing local DTD files in `DITA/dev_guide` references. The measurement above uses a large chapter map from the same Oxygen XML User Guide source repository that builds successfully in this environment.
