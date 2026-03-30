# Resolve topic fragment

- Duration: **3540 ms**
- Pipeline label: `Resolve topic fragment.`
- Module: `XmlFilterModule`

## What this step does
Normalizes topic fragments and table structures and resolves coderef via SAX filter chain.

## Where configured
`src/main/plugins/org.dita.base/build_preprocess2_template.xml` target `preprocess2.topic-fragment`.

## Notes
The step executes multiple filters in sequence and cost grows with topic count.
