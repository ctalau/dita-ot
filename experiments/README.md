# DITA-OT Experiments

This directory organizes improvement work as independent, reproducible experiments.

## Structure

- `exp-*/`: one folder per experiment.
- `AGENTS.md`: local workflow conventions for experiment work.

## How to add a new experiment

1. Create a new folder named `exp-XYZ-short-name`.
2. Add a README that documents:
   - Goal
   - What worked
   - What didn't
   - Next ideas
   - Honest assessment of direction
3. Add scripts + sample data that make the setup reproducible.
4. Keep commands deterministic and pin external versions/commits.
