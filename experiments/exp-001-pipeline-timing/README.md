# Experiment 001: Pipeline timing instrumentation

## Goal
Add and validate per-pipeline timing instrumentation, then measure a realistic run using the Oxygen XML User Guide corpus.

## What worked
- Added opt-in timing logs controlled by `dita.pipeline.timing=true`.
- Successfully built the Oxygen user guide to HTML5 and captured timing lines for each pipeline stage.
- Produced a reproducible log at `experiments/exp-000-baseline/output/timed.log`.

## What didn't
- `gradlew dist` may fail in some environments due to external AXF plug-in download failures.
- Source corpus has known warnings/errors (missing DTD/resources in some references), even though build completes.

## What we could try further
- Track timings across multiple runs and aggregate min/avg/p95.
- Split large XSLT stages to isolate hotspots.
- Add optional JSON timing output for easier CI parsing.

## Honest opinion: are we going in the right direction?
Yes. This gives us concrete stage-level numbers with very low risk and minimal code changes, which is the right first step before optimization.

## Reproducible setup

### 1) Prepare sample sources

```bash
./experiments/exp-000-baseline/scripts/setup_samples.sh
```

### 2) Build local runtime with instrumentation

```bash
./gradlew -q jar
./gradlew -q buildLocal
```

### 3) Run timed build

```bash
./experiments/exp-001-pipeline-timing/scripts/run_timed_userguide.sh
```

### Outputs
- HTML output: `experiments/exp-000-baseline/output/html5-timed/`
- Timing log: `experiments/exp-000-baseline/output/timed.log`
