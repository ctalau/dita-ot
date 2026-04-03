# Direct Inputs by Pipeline Step (HTML5 run)

This document expands each observed step into its own section and records:

1. the **direct inputs** read by that step,
2. the **code file** where the step wires/reads that input,
3. a short **snippet** from that file,
4. **how the input is processed** and which parts matter.

Scope and rule used:

- If a step reads intermediate files/job entries produced by earlier steps, that is documented as an intermediate direct input.
- Intermediate reads are **not** treated as direct dependencies on original source files.
- `ditafileset` in these Ant pipelines means "select files from the DITA-OT job inventory" (the `.job.xml` state under `${dita.temp.dir}`), typically filtered by flags such as `format`, `input`, `inputResource`, `conref`, and `processingRole`.

### Job inventory and where it is created

- The preprocess init step creates `${dita.temp.dir}/.job.xml`, which is the serialized inventory/state used by later `ditafileset` selections.

```xml
<mkdir dir="${dita.temp.dir}"/>
<echoxml file="${dita.temp.dir}/.job.xml">
  <job>
    <property name="temp-file-name-scheme">
      <string>org.dita.dost.module.reader.HashTempFileScheme</string>
```

- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`.
- The Java `Job` model reads `.job.xml` and exposes file entries/flags (format, has-conref, has-keyref, input, etc.) that pipeline modules query.

```java
private static final String JOB_FILE = ".job.xml";
private static final String ELEMENT_FILES = "files";
private static final String ELEMENT_FILE = "file";
...
if (getStore().exists(jobFile.toURI())) {
  getStore().transform(jobFile.toURI(), new JobHandler(prop, files));
}
```

- Code file: `src/main/java/org/dita/dost/util/Job.java`.

---

## Step 1 — Generate maps [2.139s] (`MapReaderModule`)

### Input: entry map (`args.input`)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<pipeline message="Generate maps" taskname="map-reader"
          inputmap="${args.input}">
  <module class="org.dita.dost.module.reader.MapReaderModule">
```
- Processing: `MapReaderModule` starts from the user input map and populates job metadata/temp-store map entries.
- Relevant parts: map hierarchy (`<map>`, `<topicref>`, map-level attributes and references) used to discover reachable content.

### Input: optional resources (`args.resources`)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<param name="resources" value="${args.resources}" if:set="args.resources"/>
```
- Processing: resource list is included in discovery/context setup for later job entries.
- Relevant parts: resource references that should be available during preprocess.

---

## Step 2 — Resolve mapref in ditamap [2.988s] (`MaprefModule`)

### Input: intermediate map files (`ditafileset format="ditamap"`)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.MaprefModule">
  <param name="style" location="${dita.plugin.org.dita.base.dir}/xsl/preprocess/mapref.xsl"/>
  <ditafileset format="ditamap" input="true"/>
  <ditafileset format="ditamap" inputResource="true"/>
</module>
```
- Processing: map files already present in the job are expanded/resolved through `mapref.xsl`.
- Relevant parts: mapref boundaries and referenced map fragments.

---

## Step 3 — Profile filtering. (maps) [0.269s] (`ProfileModule`)

### Input: intermediate map files
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.ProfileModule">
  <ditafileset format="ditamap" input="true"/>
  <ditafileset format="ditamap" inputResource="true"/>
  <param name="ditaval" location="${dita.input.valfile}" if:set="dita.input.valfile"/>
</module>
```
- Processing: module applies filtering conditions to job map entries.
- Relevant parts: map elements and attributes controlled by profiling rules.
- Yes, this step effectively means "intermediate map files **plus active filter rules**": map files are the content being filtered, and DITAVAL contributes the filter predicates.

### Input: DITAVAL file (`dita.input.valfile`, optional)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Processing: filtering rules are loaded and applied against map content.
- Relevant parts: include/exclude conditions and property values.

---

## Step 4 — Filter branches (maps) [0.332s] (`MapBranchFilterModule`)

### Input: intermediate map/job structures
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<pipeline taskname="branch-filter" message="Filter branches">
  <module class="org.dita.dost.module.filter.MapBranchFilterModule"/>
</pipeline>
```
- Processing: module uses job map graph already built by prior steps.
- Relevant parts: branch-copy/filter semantics from map branches.
- Output format: rewritten intermediate map/topic entries in temp storage and updated job inventory (`.job.xml`), not a separate standalone report file.

---

## Step 5 — Resolve keyref. (maps) [0.541s] (`KeyrefModule`)

### Input: intermediate map files
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.KeyrefModule" parallel="${parallel}">
  <ditafileset format="ditamap" input="true"/>
  <ditafileset format="ditamap" inputResource="true"/>
</module>
```
- Processing: key-space is resolved over intermediate map entries.
- Relevant parts: key definitions/usages and mapref-expanded key contributors.

---

## Step 6 — Resolve mapref in ditamap (second pass) [0.510s] (`MaprefModule`)

### Input: intermediate map files + mapref stylesheet
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<pipeline message="Resolve mapref in ditamap"
          taskname="mapref"
          unless:set="preprocess.mapref.skip">
  <module class="org.dita.dost.module.MaprefModule">
    <param name="style" location="${dita.plugin.org.dita.base.dir}/xsl/preprocess/mapref.xsl"/>
```
- Processing: a follow-up mapref expansion pass after keyref processing in map stage.
- Relevant parts: any mapref edges still requiring normalization/expansion.
- Additional effective input: the in-memory key-space built by keyref processing.
- In code, `KeyrefModule` builds the key-space from the input map and input-resource maps using `KeyrefReader`, merges scopes, and writes resulting map/job updates.

```java
final KeyrefReader reader = new KeyrefReader();
...
reader.read(job.tempDirURI.resolve(mapFile), doc);
final KeyScope startScope = reader.getKeyDefinition();
...
final KeyScope rootScope = resourceMapFis ... .reduce(startScope, KeyScope::merge);
```

- Code file: `src/main/java/org/dita/dost/module/KeyrefModule.java`.

---

## Step 7 — Resolve conref push. (maps) [0.001s] (`ConrefPushModule`)

### Input: intermediate map files
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.ConrefPushModule">
  <ditafileset format="ditamap" input="true"/>
  <ditafileset format="ditamap" inputResource="true"/>
</module>
```
- Processing: applies conref push operations over map-side intermediates.
- Relevant parts: push instructions and target locations in the map set.

---

## Step 8 — Resolve conref in input files (maps) [0.002s] (`XsltModule`)

### Input: intermediate map files with conref
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<xslt ... style="${dita.plugin.org.dita.base.dir}/xsl/preprocess/map-conref.xsl" ...>
  <ditafileset format="ditamap" conref="true" input="true"/>
  <ditafileset format="ditamap" conref="true" inputResource="true"/>
  <param name="EXPORTFILE" expression="${exportfile.url}"/>
```
- Processing: XSLT resolves conref in map intermediates.
- Relevant parts: conref source/target addressing and exported key/context data.

### Input: `${dita.temp.dir}/export.xml` (via `EXPORTFILE`)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml` (`makeurl` + `EXPORTFILE` param).
- Processing: supplemental export metadata used during conref resolution.
- Relevant parts: mapping/context data required by conref templates.

---

## Step 9 — Generate topics [16.877s] (`TopicReaderModule`)

### Input: entry map (`args.input`) to discover topics
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<pipeline message="Generate topics" taskname="topic-reader"
          inputmap="${args.input}">
  <module class="org.dita.dost.module.reader.TopicReaderModule">
```
- Processing: discovers and reads topic sources into temporary job store.
- Relevant parts: reachable topic refs from map graph; map-driven traversal scope.
- Behavior detail: this step is not a pure byte-for-byte copy. `TopicReaderModule` parses inputs, discovers referenced resources, processes waitlists, handles conref bookkeeping, and persists job state.

```java
parseInputParameters(input);
init();
readResourceFiles();
readStartFile();
processWaitList();
handleConref();
outputResult();
job.write();
```

- Code file: `src/main/java/org/dita/dost/module/reader/TopicReaderModule.java`.
- Why expensive in practice: it traverses a potentially large map/topic graph, parses many files, updates job metadata, and performs initial filtering/validation hooks while populating the temp store.

### Input: optional resources (`args.resources`)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<param name="resources" value="${args.resources}" if:set="args.resources"/>
```
- Processing: extends topic/resource discovery context.
- Relevant parts: additional referenced files to include in preprocess.

---

## Step 10 — Filter branches (topics) [0.230s] (`TopicBranchFilterModule`)

### Input: intermediate topic/job structures
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.filter.TopicBranchFilterModule"/>
```
- Processing: branch filtering logic applied to topic-side job data.
- Relevant parts: branch attributes and generated branch variants in temp state.

---

## Step 11 — Resolve conref in input files (topic-map-conref) [0.012s] (`XsltModule`)

### Input: intermediate map files with conref
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<target name="topic-map-conref" ...>
  <xslt ... style="${dita.plugin.org.dita.base.dir}/xsl/preprocess/conref.xsl" ...>
    <ditafileset format="ditamap" conref="true" input="true"/>
    <ditafileset format="ditamap" conref="true" inputResource="true"/>
```
- Processing: map-side conref resolution executed in topic phase using temp map entries.
- Relevant parts: conref links within map intermediates.

### Input: `${dita.temp.dir}/export.xml` (via `EXPORTFILE`)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Processing: shared export context for conref templates.
- Relevant parts: exported lookup/context values.

---

## Step 12 — Resolve keyref. (topics) [3.792s] (`KeyrefModule`)

### Input: intermediate topic files (`ditafileset format="dita"`)
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.KeyrefModule" parallel="${parallel}">
  <ditafileset format="dita"/>
</module>
```
- Processing: resolves keyrefs in topic intermediates against built key-space.
- Relevant parts: keyref-bearing elements and resulting resolved text/targets.
- Additional effective input: the key-space object (`KeyScope`) built from map key definitions and keyscope hierarchy by `KeyrefReader`.

```java
reader.read(job.tempDirURI.resolve(mapFile), doc);
final KeyScope startScope = reader.getKeyDefinition();
...
rootScope = resolveIntermediate(keyScopeWithParents);
```

- Code files: `src/main/java/org/dita/dost/module/KeyrefModule.java`, `src/main/java/org/dita/dost/reader/KeyrefReader.java`.

---

## Step 13 — Resolve copy-to. [0.307s] (`CopyToModule`)

### Input: intermediate job metadata + optional `force-unique`
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.CopyToModule">
  <param name="force-unique" value="${force-unique}" if:set="force-unique"/>
</module>
```
- Processing: computes/applies copy-to resolution over job entries.
- Relevant parts: copy-to mappings, uniqueness behavior, target naming.

---

## Step 14 — Resolve conref push. (topics) [0.001s] (`ConrefPushModule`)

### Input: intermediate topic and map files
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.ConrefPushModule">
  <ditafileset format="dita"/>
  <ditafileset format="ditamap"/>
</module>
```
- Processing: applies conref push over topic/map intermediates.
- Relevant parts: push source directives and affected target nodes.

---

## Step 15 — Resolve conref in input files (topics) [10.517s] (`XsltModule`)

### Input: intermediate topic/map files with conref
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<xslt ... style="${dita.plugin.org.dita.base.dir}/xsl/preprocess/conref.xsl" ...>
  <ditafileset conref="true" format="dita"/>
  <ditafileset conref="true" format="ditamap"/>
  <param name="EXPORTFILE" expression="${exportfile.url}"/>
```
- Processing: resolves conref links in topic/map intermediates.
- Relevant parts: conref addressing plus export-derived resolution context.
- Caching suggestion for conref dependencies:
  - Build an index keyed by **consumer file URI** -> set of referenced **conref target URIs + fragment IDs**.
  - Include `conrefend` ranges and indirect chains in the closure.
  - Invalidate a consumer when any target in its closure changes (content hash or mtime), and recompute closure if map/key context changes.
  - Persist the index as a sidecar (for example JSON in temp/cache dir) keyed by pipeline config hash (`transtype`, filter file hash, include.rellinks, etc.).

### Input: `${dita.temp.dir}/export.xml`
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml` (`makeurl` + `EXPORTFILE`).
- Processing: supplies supplemental lookup/context data.
- Relevant parts: exported structures used by conref templates.
- Export format note: this is an XML file (`export.xml`). In core code it is described as "*export.xml to store exported elements*".

```java
/**export.xml to store exported elements.*/
public static final String FILE_NAME_EXPORT_XML = "export.xml";
```

- Code file: `src/main/java/org/dita/dost/util/Constants.java`.

---

## Step 16 — Resolve topic fragment. [3.540s] (`XmlFilterModule` / SAX chain)

### Input: intermediate topic files
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<sax parallel="${parallel}">
  <ditafileset format="dita"/>
  <filter class="org.dita.dost.writer.TopicFragmentFilter">
  ...
  <filter class="org.dita.dost.writer.CoderefResolver" ...>
```
- Processing: runs filter chain to normalize fragments/tables and resolve coderef.
- Relevant parts: fragment IDs, table structures, coderef/codeblock-related markup.

---

## Step 17 — Process chunks. [0.040s] (`ChunkModule`)

### Input: intermediate job map/topic structures
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<pipeline message="Process chunks." taskname="chunk">
  <module class="org.dita.dost.module.ChunkModule">
    <param name="transtype" value="${transtype}"/>
```
- Processing: chunking decisions are applied over current job graph.
- Relevant parts: chunk attributes and map/topic relationships that drive split/merge behavior.

---

## Step 18 — Process DITA 2 chunks. [1.429s] (`org.dita.dost.chunk.ChunkModule`)

### Input: intermediate job map/topic structures
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<pipeline message="Process DITA 2 chunks." taskname="chunk2">
  <module class="org.dita.dost.chunk.ChunkModule">
```
- Processing: second chunk pipeline for DITA 2 behavior.
- Relevant parts: DITA 2 chunk semantics and root-chunk overrides.

---

## Step 19 — Move metadata entries. [11.544s] (`MoveMetaModule`)

### Input: intermediate map/topic relations
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.MoveMetaModule">
  <param name="style" location="${dita.plugin.org.dita.base.dir}/xsl/preprocess/mappull.xsl"/>
```
- Processing: metadata is moved/pulled according to map/topic context using `mappull.xsl`.
- Relevant parts: metadata-bearing elements and map linkage used for propagation.
- Example:
  - If map `topicref` provides metadata (for example `navtitle`, audience/product props), `MoveMetaModule` pushes/inserts that metadata into referenced topic/map targets via `DitaMetaWriter`/`DitaMapMetaWriter`.
  - The same module also pulls topic-derived metadata back to map context using the configured stylesheet.

```java
final Map<URI, Map<String, Element>> mapSet = getMapMetadata(fis);
pushMetadata(mapSet);
pullTopicMetadata(input, fis);
```

- Code file: `src/main/java/org/dita/dost/module/MoveMetaModule.java`.

---

## Step 20 — Move related links [5.094s] (`MoveLinksModule`)

### Input: intermediate relationship/link graph
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<module class="org.dita.dost.module.MoveLinksModule">
  <param name="style" location="${dita.plugin.org.dita.base.dir}/xsl/preprocess/maplink.xsl"/>
  <param name="include.rellinks" expression="${include.rellinks}" if:set="include.rellinks"/>
</module>
```
- Processing: related links are generated/relocated using map relationship data.
- Relevant parts: relationship tables and link scopes affected by `include.rellinks`.
- Where graph is generated: in this step, `MoveLinksModule` runs `maplink.xsl` against the input map to produce link mapping data.
- Storage/format: the mapping is an in-memory DOM + Java map structure (`Map<File, Map<String, Element>>`), not a separate persisted graph file.
- Output of this step: topic files in temp storage are rewritten in-place by `DitaLinksWriter` with inserted/relocated related links.

```java
transformer.setSource(source);
transformer.setDestination(result);
transformer.transform();
final Map<File, Map<String, Element>> mapSet = getMapping(doc);
...
linkInserter.setCurrentFile(uri);
linkInserter.write(new File(uri));
```

- Code file: `src/main/java/org/dita/dost/module/MoveLinksModule.java`.

---

## Step 21 — Pull metadata for link and xref element [8.301s] (`XsltModule` / `topicpull.xsl`)

### Input: intermediate normal-role topic files
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<xslt ... style="${dita.plugin.org.dita.base.dir}/xsl/preprocess/topicpull.xsl" ...>
  <ditafileset format="dita" processingRole="normal"/>
  <param name="TABLELINK" expression="${args.tablelink.style}" .../>
  <param name="FIGURELINK" expression="${args.figurelink.style}" .../>
```
- Processing: enriches links/xrefs with pulled metadata.
- Relevant parts: xref/link elements, generated link text, table/figure link style knobs.
- Output: rewritten intermediate topic files in `${dita.temp.dir}` (same logical files, updated content), which become inputs for later clean/html5 steps.

---

## Step 22 — Clean ditamap [0.383s] (`XsltModule` / `clean-map.xsl`)

### Input: intermediate map files
- Code file: `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- Snippet:
```xml
<xslt ... style="${dita.plugin.org.dita.base.dir}/xsl/preprocess/clean-map.xsl">
  <ditafileset format="ditamap"/>
```
- Processing: cleans/normalizes map intermediates before final preprocess cleanup.
- Relevant parts: map structures that must be normalized for downstream output.

---

## Step 23 — Clean preprocess [4.288s] (`CleanPreprocessModule`)

### Input: temp job outputs + optional rewrite rule configuration
- Code file: `src/main/plugins/org.dita.base/build_preprocess_template.xml`
- Snippet:
```xml
<pipeline message="Clean preprocess" taskname="clean-preprocess">
  <module class="org.dita.dost.module.CleanPreprocessModule" parallel="${parallel}">
    <param name="result.rewrite-rule.xsl" value="${result.rewrite-rule.xsl}" if:set="result.rewrite-rule.xsl"/>
```
- Processing: rewrites/finalizes preprocess artifacts and normalized temp layout.
- Relevant parts: job file inventory and rewrite rules that alter result paths/content.

---

## Step 24 — Read image metadata [3.374s] (`ImageMetadataModule`)

### Input: intermediate job image references (and referenced image binaries)
- Code file: `src/main/plugins/org.dita.html5/build_dita2html5.xml`
- Snippet:
```xml
<pipeline message="Read image metadata." taskname="image-metadata">
  <module class="org.dita.dost.module.ImageMetadataModule" parallel="${parallel}">
    <param name="outputdir" location="${dita.output.dir}"/>
```
- Processing: resolves image references and extracts metadata stored for later rendering.
- Relevant parts: image dimensions/metadata used by topic HTML generation.

---

## Step 25 — Convert DITA topic to HTML5 [32.958s] (`XsltModule`)

### Input: intermediate normal-role topic files
- Code file: `src/main/plugins/org.dita.html5/build_dita2html5.xml`
- Snippet:
```xml
<xslt destdir="${dita.output.dir}" ... style="${args.xsl}" ...>
  <ditafileset format="dita" processingRole="normal"/>
  <param name="FILTERFILE" expression="${dita.input.valfile.url}" if:set="dita.input.valfile"/>
  <param name="CSS" expression="${args.css.file}" if:set="args.css.file"/>
  <param name="HDF" expression="${args.hdf.url}" if:set="args.hdf.url"/>
```
- Processing: transforms preprocessed topic XML into HTML5 output files.
- Relevant parts: fully preprocessed topic content plus optional rendering/config params.

### Input: transform stylesheet (`${args.xsl}`)
- Code file: `src/main/plugins/org.dita.html5/build_dita2html5.xml`
- Snippet:
```xml
<property name="args.xsl" value="${dita.plugin.org.dita.html5.dir}/xsl/dita2html5.xsl"/>
```
- Processing: controls topic-to-HTML template logic.
- Relevant parts: template rules for elements, links, metadata, and output markup.
- Output cardinality note: this is generally one output HTML file per **input topic file selected in the job fileset** (via mapper/extension), not strictly one output per topic element. Earlier chunk/copy-to processing can change which files exist in that selected input set.

### Input: optional DITAVAL/header/footer/CSS parameter files
- Code file: `src/main/plugins/org.dita.html5/build_dita2html5.xml` (`html5.init` + `html5.topics` macro).
- Processing: passed as XSLT params; they influence conditional rendering and output decoration.
- Relevant parts: filter values, header/footer fragments, and CSS filename/path values.

---

## Step 26 — Convert DITA map to HTML5 [1.113s] (`XsltModule`)

### Input: intermediate input map files (`ditafileset input="true" format="ditamap"`)
- Code file: `src/main/plugins/org.dita.html5/build_dita2html5.xml`
- Snippet:
```xml
<xslt destdir="${html5.toc.output.dir}" style="${args.html5.toc.xsl}">
  <ditafileset input="true" format="ditamap"/>
  <param name="FILTERFILE" expression="${dita.input.valfile.url}" if:set="dita.input.valfile"/>
```
- Processing: map intermediates are transformed into HTML5 TOC/cover output.
- Relevant parts: map navigation structure and map metadata used by TOC templates.

### Input: map transform stylesheet (`${args.html5.toc.xsl}`)
- Code file: `src/main/plugins/org.dita.html5/build_dita2html5.xml`
- Snippet:
```xml
<property name="args.html5.toc.xsl" value="${dita.plugin.org.dita.html5.dir}/xsl/map2html5-cover.xsl"/>
```
- Processing: defines map-to-HTML5 TOC generation rules.
- Relevant parts: template logic for navigation tree and merged output file.

---

## Primary configuration references used

- `docs/pipeline-steps.md`
- `src/main/plugins/org.dita.base/build_preprocess2_template.xml`
- `src/main/plugins/org.dita.base/build_preprocess_template.xml`
- `src/main/plugins/org.dita.html5/build_dita2html5.xml`
