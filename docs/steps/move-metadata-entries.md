# Move metadata entries

- Duration: **11544 ms**
- Pipeline label: `Move metadata entries.`
- Module: `MoveMetaModule`

## What this step does
Pulls and moves metadata entries according to map/topic relationships.

## Where configured
`src/main/plugins/org.dita.base/build_preprocess2_template.xml` target `topic-move-meta-entries`.

## Notes
This is a high-cost step in this run and a strong candidate for optimization work.
