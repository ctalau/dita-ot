# Resolve conref in input files (topics)

- Duration: **10517 ms**
- Pipeline label: `Resolve conref in input files`
- Module: `XsltModule`

## What this step does
Runs conref resolution over topic and map temporary files using XSLT.

## Where configured
`src/main/plugins/org.dita.base/build_preprocess2_template.xml` target `topic-conref`.

## Notes
Conref-heavy sources can make this one of the most expensive preprocessing stages.
