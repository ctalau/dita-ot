# Generate topics

- Duration: **16877 ms**
- Pipeline label: `Generate topics`
- Module: `TopicReaderModule`

## What this step does
Reads topic files into temporary storage for map-first preprocessing.

## Where configured
`src/main/plugins/org.dita.base/build_preprocess2_template.xml` target `topic-reader`.

## Notes
This is one of the largest preprocessing costs and scales with corpus size and map reachability.
