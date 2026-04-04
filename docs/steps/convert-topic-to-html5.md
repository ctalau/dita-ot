# Convert DITA topic to HTML5

- Duration: **32958 ms**
- Pipeline label: `Convert DITA topic to HTML5`
- Module: `XsltModule`

## What this step does
Step 25 runs one XSLT transform per `format="dita"` + `processingRole="normal"` file in the job, writing HTML files to `${dita.output.dir}` through `JobMapper` extension remapping. The actual stylesheet entry point is `xsl/dita2html5.xsl` (via `${args.xsl}`).

## Where configured
- `src/main/plugins/org.dita.html5/build_dita2html5.xml`
  - target/macro: `html5.topics`
  - pipeline message: `Convert DITA topic to HTML5`
  - source fileset: `<ditafileset format="dita" processingRole="normal"/>`
  - main stylesheet: `${dita.plugin.org.dita.html5.dir}/xsl/dita2html5.xsl`

---

## Step 25 walkthrough (focused on extra files loaded)

Below, “loaded” means either:
1. stylesheet modules compiled via `xsl:import`/`xsl:include`, or
2. runtime document reads (`document()`/`doc()`) done while producing output.

### 1) Ant config resolves optional external files and passes them into XSLT
Before transformation, `html5.init` validates and URL-normalizes optional user-supplied files:
- `args.hdf` (head fragment)
- `args.hdr` (running header fragment)
- `args.ftr` (running footer fragment)

Then `html5.topics` passes these into XSLT as parameters (`HDF`, `HDR`, `FTR`), along with optional `FILTERFILE` (DITAVAL), `input.map.url`, CSS parameters, and output path params.

### 2) Stylesheet compilation loads the HTML5 transform stack
The top-level stylesheet imports `dita2html5Impl.xsl`, which imports most of the HTML5 and common base transform modules.

High-value loaded modules (not exhaustive of every template in each file):
- Base/common runtime helpers:
  - `plugin:org.dita.base:xsl/common/output-message.xsl`
  - `plugin:org.dita.base:xsl/common/dita-utilities.xsl`
  - `plugin:org.dita.base:xsl/common/related-links.xsl`
  - `plugin:org.dita.base:xsl/common/dita-textonly.xsl`
- HTML5 topic and domain renderers:
  - `topic.xsl`, `concept.xsl`, `task.xsl`, `reference.xsl`, domain files (`pr-d.xsl`, `ui-d.xsl`, `hazard-d.xsl`, etc.)
- Navigation support:
  - `nav.xsl` (imports `map2html5Impl.xsl`)
- Topic-level includes from `topic.xsl`:
  - `get-meta.xsl`, `rel-links.xsl`, `tables.xsl`, `simpletable.xsl`, `css-class.xsl`, `functions.xsl`

### 3) Runtime reads beyond the input topic itself
These are the important non-topic files this step may open at runtime:

#### A. DITAVAL filter file (if set)
- Loaded from param `FILTERFILE` in `topic.xsl` using `document($FILTERFILEURL, /)`.
- Used for passthrough-style/flagging behavior.

#### B. User HTML fragments (if set)
- Head fragment via `document($HDFFILE, /)`.
- Running header via `document($HDRFILE, /)`.
- Running footer via `document($FTRFILE, /)`.

#### C. Input map file (if nav TOC is enabled)
- `nav.xsl` loads `document($input.map.url)` and normalizes it for TOC generation.
- This is the key extra input when `nav-toc=partial|full`.

#### D. Localization/message resources
- `output-message.xsl` loads `platform:config/messages.xml` for message text/severity lookup.
- `dita-utilities.xsl` loads `plugin:org.dita.base:xsl/common/strings.xml` (language list), then loads referenced per-language string files with `document(., $variableFiles[1])`.

#### E. Hazard symbol SVG asset
- `hazard-d.xsl` parameter `inline-hazard-svg` defaults to loading `plugin:org.dita.html5:resources/ISO_7010_W001_html.svg`.

#### F. Referenced glossary targets for term/abbreviated-form rendering (conditional)
This is **not a general link-resolution pass** (that work is mostly done earlier in preprocess). In Step 25, `dita-ot:retrieve-href-target()` is used on specific render-time paths for glossary display text:

- Code path 1: `topic.xsl` template `match="*[contains(@class, ' topic/term ')]"` (when both `@keyref` and `@href` are present, and local scope) calls `dita-ot:retrieve-href-target(@href)` to load glossary entry content used to choose rendered surface form/acronym.
- Code path 2: `abbrev-d.xsl` template `match="*[contains(@class,' abbrev-d/abbreviated-form ')]"` (when both `@keyref` and `@href` are present) also calls `dita-ot:retrieve-href-target(@href)` to validate/read glossentry content.
- Function implementation: `src/main/plugins/org.dita.base/xsl/common/functions.xsl` resolves the URI and then uses `doc()` (guarded by `doc-available()`) to open the target document.
- Scope note: within the HTML5 Step 25 transform stack, these are the only direct call sites of `dita-ot:retrieve-href-target()` (`topic.xsl` and `abbrev-d.xsl`). The same helper is also used by the XHTML transform plugin, but that is a different output pipeline/step.

---

## Practical reading of step-25 cost
If you are profiling step 25 and ignoring “just traverse the topic” template cost, the highest-signal extra I/O to inspect first is:
1. map load/normalization (`input.map.url`) when nav TOC is on,
2. DITAVAL + header/footer fragment reads,
3. localization/message resource loads,
4. glossary-target `doc()` loads used by term/abbreviated-form rendering.

## Notes
This is still the single most expensive step in the observed run, and its runtime varies significantly with optional file-driven features (TOC mode, filtering, fragment injection, reference resolution fan-out).
