# Oxygen User Guide HTML5 Pipeline Steps

This document lists the observed pipeline steps for building:

- Input: `samples/oxygenxml-userguide/DITA/UserManual.ditamap`
- Transform: `html5`
- Timing source: `experiments/exp-000-baseline/output/timed.log`

## Full ordered step list

| # | Step | Module(s) | Duration |
|---|---|---|---:|
| 1 | Generate maps | MapReaderModule | 2139 ms |
| 2 | Resolve mapref in ditamap | MaprefModule | 2988 ms |
| 3 | Profile filtering. | ProfileModule | 269 ms |
| 4 | Filter branches | MapBranchFilterModule | 332 ms |
| 5 | Resolve keyref. | KeyrefModule | 541 ms |
| 6 | Resolve mapref in ditamap | MaprefModule | 510 ms |
| 7 | Resolve conref push. | ConrefPushModule | 1 ms |
| 8 | Resolve conref in input files | XsltModule | 2 ms |
| 9 | Generate topics | TopicReaderModule | 16877 ms |
| 10 | Filter branches | TopicBranchFilterModule | 230 ms |
| 11 | Resolve conref in input files | XsltModule | 12 ms |
| 12 | Resolve keyref. | KeyrefModule | 3792 ms |
| 13 | Resolve copy-to. | CopyToModule | 307 ms |
| 14 | Resolve conref push. | ConrefPushModule | 1 ms |
| 15 | Resolve conref in input files | XsltModule | 10517 ms |
| 16 | Resolve topic fragment. | XmlFilterModule | 3540 ms |
| 17 | Process chunks. | ChunkModule | 40 ms |
| 18 | Process DITA 2 chunks. | ChunkModule | 1429 ms |
| 19 | Move metadata entries. | MoveMetaModule | 11544 ms |
| 20 | Move related links | MoveLinksModule | 5094 ms |
| 21 | Pull metadata for link and xref element | XsltModule | 8301 ms |
| 22 | Clean ditamap | XsltModule | 383 ms |
| 23 | Clean preprocess | CleanPreprocessModule | 4288 ms |
| 24 | Read image metadata. | ImageMetadataModule | 3374 ms |
| 25 | Convert DITA topic to HTML5 | XsltModule | 32958 ms |
| 26 | Convert DITA map to HTML5 | XsltModule | 1113 ms |

## Steps over 3 seconds

Detailed docs for steps above 3000 ms:

- [Generate topics](steps/generate-topics.md)
- [Resolve keyref (topics)](steps/resolve-keyref-topics.md)
- [Resolve conref in input files (topics)](steps/resolve-conref-topics.md)
- [Resolve topic fragment](steps/resolve-topic-fragment.md)
- [Move metadata entries](steps/move-metadata-entries.md)
- [Move related links](steps/move-related-links.md)
- [Pull metadata for link and xref element](steps/pull-metadata-topicpull.md)
- [Clean preprocess](steps/clean-preprocess.md)
- [Read image metadata](steps/read-image-metadata.md)
- [Convert DITA topic to HTML5](steps/convert-topic-to-html5.md)
