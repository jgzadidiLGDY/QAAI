# Phase 6 Closeout

Phase 6 added single-call AI-assisted analysis from captured transcript
artifacts.

## Completed

- Added `--analyze-call --call-id=<local_call_id>`.
- Added analysis domain models for reports, findings, and evidence.
- Added an OpenAI-backed analysis client.
- Added deterministic prompt construction from scenario and transcript data.
- Required captured `transcript.json` before analysis.
- Wrote `analysis.json`.
- Wrote `analysis.md`.
- Updated metadata with `analysis_json` and `analysis_markdown` paths.
- Appended analysis entries to `manifest.json` when present.
- Validated that findings include transcript evidence.
- Validated evidence quotes against normalized transcript turns.
- Required AI reports to mark `human_review_required = true`.
- Added tests for prompt construction, artifact writing, and evidence validation.

## Local Usage

After starting and capturing a Retell call:

```powershell
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

## Expected Artifacts

```text
outputs/{call_id}/analysis.json
outputs/{call_id}/analysis.md
outputs/{call_id}/metadata.json
outputs/{call_id}/manifest.json
```

`manifest.json` is updated only when it already exists.

## Still Out Of Scope

- Automated pass/fail decisions.
- Batch analysis.
- Cross-run issue clustering.
- Webhook-triggered analysis.
- Scenario mutation from AI output.

## Tests

```powershell
.\gradlew test
```

## Next Phase

Phase 7 should make runs easier to repeat and compare, likely by adding a run
index and reproducibility checks across captured artifact bundles.
