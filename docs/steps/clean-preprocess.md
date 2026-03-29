# Clean preprocess

- Duration: **4288 ms**
- Pipeline label: `Clean preprocess`
- Module: `CleanPreprocessModule`

## What this step does
Rewrites temporary outputs into final normalized temp layout and applies rewrite rules.

## Where configured
`src/main/plugins/org.dita.base/build_preprocess_template.xml` target `clean-preprocess`.

## Notes
This step is non-trivial for large projects due to high file counts.
