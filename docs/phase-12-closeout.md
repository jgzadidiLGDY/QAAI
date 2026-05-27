# Phase 12 Closeout

Phase 12 closed the MVP+ documentation and artifact-trust loop without adding
new workflow automation or changing pass/fail ownership.

## Completed

- Added operator-focused setup and command workflow documentation.
- Added run lifecycle, status, completeness, and troubleshooting documentation.
- Added additive reproducibility metadata to newly written `metadata.json`.
- Recorded the command, app version, and optional Git commit for dry-run,
  capture, and analysis metadata writes.
- Kept older metadata readable when `reproducibility` is absent.
- Preserved AI-assisted analysis as advisory and human-reviewed.

## Local Usage

Operator guide:

```text
docs/operator-guide.md
```

Run lifecycle and artifact trust:

```text
docs/run-lifecycle.md
```

Inspect one run:

```powershell
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
```

## Still Out Of Scope

- Batch capture or batch analysis.
- Issue clustering across runs.
- Webhook-driven retries.
- Automated pass/fail decisions.
- Silver task packaging.

## Tests

```powershell
.\gradlew test --tests "*DryRunRunnerTest" --tests "*ArtifactCaptureServiceTest" --tests "*AnalysisServiceTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Base commit: `6cda895`
- Fix commit: `52d0353`

The candidate behavior is reproducibility metadata:
newly written metadata records which local command produced the current state,
the app version, and a best-effort Git commit while preserving compatibility
with older metadata.
