# Phase 22 Closeout

Phase 22 polished the current implementation into the v1.0.0 review candidate.

## Completed

- Shortened `README.md` into a v1 reviewer entry point.
- Moved detailed phase-history navigation to `AI_native_builder_journal.md`.
- Added `docs/v1-review-guide.md`.
- Added `docs/v1-release-notes.md`.
- Documented v1 release polish in project specs and architecture docs.
- Marked runtime and build version metadata as `1.0.0`.
- Updated version-related tests and documentation examples.
- Added GitHub Actions CI for the Gradle test suite.
- Recorded Phase 22 base/fix hashes in `docs/silver-readiness.md` after each
  coherent commit.

## v1 Review Path

```powershell
.\gradlew test
.\gradlew bootRun --args="--suite=suites/receptionist-smoke.yaml"
.\gradlew bootRun --args="--generate-report"
```

Inspect:

```text
outputs/suites/{suite_run_id}/suite-report.md
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
outputs/reports/{report_id}/index.html
```

## Still Out Of Scope

- Database storage.
- New interactive web dashboard.
- Hosted deployment.
- Automatic pass/fail decisions.
- New AI behavior.
- Live Retell batch execution changes.
- Generated scenario promotion.
- Project Silver task authoring.

## Tests

Focused version-metadata tests:

```powershell
.\gradlew test --tests "*RuntimeReproducibilityMetadataTest" --tests "*DryRunRunnerTest" --tests "*RetellCallRunnerTest" --tests "*ArtifactCaptureServiceTest" --tests "*AnalysisServiceTest"
```

Full suite:

```powershell
.\gradlew test
```

Both passed locally.

## Silver Notes

Phase 22 contains mostly docs and packaging candidates. The strongest candidate
is the v1.0.0 runtime version metadata change because it has deterministic tests
against generated reproducibility metadata.

Silver remains secondary context. The v1 product direction is still governed by
QAAI's local-first, artifact-first, human-reviewed workflow.
