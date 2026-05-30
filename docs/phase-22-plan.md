# Phase 22 Plan

## Summary

Phase 22 turns the currently implemented QAAI feature set into the first major
reviewable release: v1.0.0.

This is a release-polish phase, not a new product-capability phase. The goal is
to make the repository easy to evaluate on GitHub by presenting the existing
local-first workflow clearly: deterministic scenarios and suites, inspectable
artifacts, advisory analysis/evaluation/review outputs, and generated reports.

## Implementation Plan

1. Reframe project docs around v1 as the current milestone.
2. Shorten the README so new reviewers see setup, quickstart, and the v1
   workflow before phase history.
3. Link detailed phase history through `AI_native_builder_journal.md` and
   phase closeout docs instead of duplicating it in the README.
4. Add a concise v1 review guide with a reproducible local reviewer path.
5. Mark the application version as `1.0.0` in build and example configuration.
6. Add v1 release notes or changelog content.
7. Add a GitHub Actions CI workflow for the Gradle test suite.
8. Run the full test suite before closeout.

## Likely Files

- `README.md`
- `AI_native_builder_journal.md`
- `build.gradle`
- `.env.example`
- `docs/project_specs.md`
- `docs/architecture.md`
- `docs/operator-guide.md`
- `docs/v1-review-guide.md`
- `docs/v1-release-notes.md`
- `.github/workflows/ci.yml`
- `docs/silver-readiness.md`

## Tests

- Run the full unit test suite:

```powershell
.\gradlew test
```

- Optional reviewer smoke commands after implementation:

```powershell
.\gradlew bootRun --args="--suite=suites/receptionist-smoke.yaml"
.\gradlew bootRun --args="--generate-report"
```

The smoke commands create local artifacts under `outputs/`, which remain
ignored by Git.

## Assumptions

- v1 means the current implemented workflow is ready for review, not that the
  product is complete.
- The v1 persistence layer remains local filesystem artifacts under `outputs/`.
- The existing static report is the v1 review surface.
- A database and web dashboard should wait until a later milestone proves the
  need for hosted search, multi-user review, or richer interaction.
- Project Silver extraction rules are secondary. They shape commit boundaries
  and future task notes, but they do not define QAAI product scope.

## Risks

- The README can become too long if it keeps duplicating phase closeout history.
- Version changes may affect tests or reproducibility metadata if assertions
  expect the old snapshot value.
- CI can drift from local Windows-oriented documentation if commands are not
  kept Gradle-wrapper based and platform-neutral.
- Docs-only changes are useful for reviewability but weak Silver candidates.

## Scope Boundaries

Phase 22 should not include:

- database storage
- a new interactive web UI
- hosted deployment
- new AI behavior
- automatic pass/fail decisions
- live Retell batch execution changes
- generated scenario promotion
- broad artifact migration
- Project Silver task authoring

## Project Silver Notes

Project Silver remains secondary context only.

For Phase 22, useful Silver-aligned constraints are:

- keep release-polish commits strictly separate
- avoid mixing README cleanup with versioning or CI behavior
- record base/fix hashes after each coherent commit
- mark docs-only candidates as weak
- prefer deterministic runtime behavior for any future strong candidates

Phase 22 itself is mostly packaging and documentation, so most commits are
expected to be weak Silver candidates unless they add verifiable CI or
runtime-version behavior.
