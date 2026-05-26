# Phase 7 Closeout

Phase 7 added reproducible run indexing and artifact completeness checks.

## Completed

- Added `outputs/index.jsonl` as an append-only run lifecycle index.
- Appended index entries after successful artifact writes.
- Added status-aware artifact completeness checks.
- Treated unavailable Retell audio as a warning, not a hard failure.
- Added `--list-runs` for local run inspection.
- Added tests for completeness rules, JSONL append behavior, and writer integration.

## Local Usage

List indexed runs:

```powershell
.\gradlew bootRun --args="--list-runs"
```

Any successful dry run, Retell call start, artifact capture, or analysis write
appends an entry to:

```text
outputs/index.jsonl
```

## Expected Artifacts

```text
outputs/index.jsonl
outputs/{call_id}/metadata.json
outputs/{call_id}/scenario.yaml
outputs/{call_id}/observations.md
```

Additional expected artifacts depend on run status:

- dry run: `transcript.txt`, `patient_simulation.md`
- captured Retell run: `transcript.json`, `transcript.txt`, `manifest.json`
- analyzed run: `analysis.json`, `analysis.md`

## Still Out Of Scope

- Automated pass/fail decisions.
- Batch analysis.
- Cross-run issue clustering.
- Web UI or searchable database.
- Webhook-triggered indexing.

## Tests

```powershell
.\gradlew test
```

## Next Phase

Phase 8 should return to conversation-quality iteration using real captured run
history and observations as the review substrate.
