# Experiment 000: Baseline run with Oxygen XML User Guide content

## Goal

Establish a baseline, reproducible way to run DITA-OT using a realistic documentation corpus (Oxygen XML User Guide sources). This gives us a stable starting point for future optimization and quality experiments.

## What worked

- We can reproducibly fetch the sample corpus via pinned commit.
- We can execute a baseline build with DITA-OT from this repository.
- We can capture outputs and logs for later comparison.

## What didn't

- This baseline does not yet include performance instrumentation (CPU/memory/phase timing).
- It does not yet compare output correctness against a golden baseline.

## What we could try further

- Add timing + resource profiling around each pipeline stage.
- Add output-diff checks to detect regressions.
- Create alternative experiment folders for:
  - plugin configuration changes,
  - preprocessing tuning,
  - parallelism/JVM option tuning,
  - custom transform experiments.

## Honest opinion: are we going in the right direction?

Yes, for an initial step. A pinned-sample baseline is the right foundation because it makes later changes measurable and comparable. However, this is only setup work; we should quickly follow with experiments that produce hard numbers and quality diffs.

## Reproducible setup

### 1) Fetch sample sources

```bash
./experiments/exp-000-baseline/scripts/setup_samples.sh
```

### 2) Run baseline build

```bash
./experiments/exp-000-baseline/scripts/run_baseline.sh
```

### Output locations

- Build output: `experiments/exp-000-baseline/output/`
- Build log: `experiments/exp-000-baseline/output/baseline.log`
