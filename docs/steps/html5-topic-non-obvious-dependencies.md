# HTML5 topic transform: non-obvious dependencies

This note captures dependencies that can influence a single topic's HTML output even when the topic source file itself is unchanged.

## 1) Root map and map context

- The HTML5 topic step initializes `html5.map.url` from the input ditamap.
- Topic rendering receives map-aware context (`input.map.url`) via extension params.
- Practical effect: map reorganization can change navigation, parent/child context, and related link output for topics.

## 2) Key-space contributors (not only direct key definitions)

- Topic keyref resolution depends on the complete key space built during preprocess.
- Any map/topic that contributes key definitions (including keys pulled through mapref) can change resolved links/text in a topic.

## 3) Conref / conkeyref source closure

- Conref is resolved in preprocess over topic and map temp files before HTML conversion.
- A topic can change because *referenced* source files changed, not because the topic file changed.

## 4) Link and metadata enrichment stages

- `MoveLinksModule` rewrites/relocates related links based on map/link graph state.
- `topicpull.xsl` populates metadata used for link/xref rendering.
- Practical effect: updates in linked targets can alter generated link text/metadata in the rendered topic.

## 5) Profiling/filter artifacts

- `FILTERFILE` (DITAVAL) is passed to HTML5 topic XSLT.
- Conditional processing changes effective content before/at rendering.
- Any filter value or file change can alter output bytes for a topic.

## 6) Header/footer and styling resources passed as params

- Header/footer resources (`HDF`, `HDR`, `FTR`) are passed to transform params.
- CSS path/file params can impact generated output references and classes.
- These files/paths are easy to miss when reasoning only about DITA source deps.

## 7) Image resources through metadata side-channel

- The image metadata pipeline runs before topic conversion.
- Image file changes (dimensions/metadata) can influence generated HTML attributes/markup.

## 8) Generated preprocess intermediates

- HTML5 topic conversion runs after many preprocess stages (keyref, conref, maplink, topicpull, clean preprocess).
- Effective dependency closure is often the set of preprocess inputs that feed those stages, not just the final temp topic file.

## Suggested cache-design implication

For safe invalidation, track:

1. The topic source file.
2. Root map and mapref-expanded key-space contributors.
3. Conref/conkeyref source closure.
4. Inputs to maplink/topicpull enrichment.
5. Filter/config/header/footer files passed as params.
6. Referenced image files that contribute metadata.
