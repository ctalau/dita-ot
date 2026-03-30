# Pull metadata for link and xref element

- Duration: **8301 ms**
- Pipeline label: `Pull metadata for link and xref element`
- Module: `XsltModule`

## What this step does
Runs `topicpull.xsl` to populate link/xref metadata in topic files.

## Where configured
`src/main/plugins/org.dita.base/build_preprocess2_template.xml` target `topic-topicpull`.

## Notes
This stage is consistently heavy on large content sets.
