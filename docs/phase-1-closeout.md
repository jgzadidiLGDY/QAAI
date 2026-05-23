# Phase 1 Closeout

Phase 1 added the deterministic scenario runner for local dry runs.

## Completed

- Added the first scenario YAML fixture.
- Added scenario model, loader, and validator.
- Added deterministic dry-run execution.
- Added artifact writing under `outputs/{call_id}/`.
- Added metadata that explicitly marks `run_mode` as `dry_run`.
- Added dry-run transcript generation from ordered scenario steps.
- Added tests for loading, validation, artifact writing, and runner behavior.

## Local Usage

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
```

## Expected Artifacts

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
```

## Still Out Of Scope

- Retell API calls.
- Real outbound calls.
- Audio recording collection.
- Retell transcript capture.
- OpenAI analysis.
- Human pass/fail decisions.

## Next Phase

Phase 2 should improve conversation-quality guidance while preserving the same
deterministic dry-run execution and local `call_id` artifact model.
