# Resolve keyref (topics)

- Duration: **3792 ms**
- Pipeline label: `Resolve keyref.`
- Module: `KeyrefModule`

## What this step does
Resolves key references inside topic content after topic discovery.

## Where configured
`src/main/plugins/org.dita.base/build_preprocess2_template.xml` target `topic-keyref`.

## Notes
Key resolution depends on full key space and can expand with map complexity.
