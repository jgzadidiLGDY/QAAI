# Phase 2 Closeout

Phase 2 added deterministic conversation-quality guidance to local dry runs.

## Completed

- Added `conversation_quality` fields to the scenario schema.
- Added validation for required conversation-quality guidance.
- Updated the appointment reschedule scenario with welcome, initiative, pacing,
  clarification, and expected-risk guidance.
- Added conversation-quality guidance to dry-run transcripts.
- Added deterministic `observations.md` artifacts under `outputs/{call_id}/`.
- Added metadata links for `observations.md`.
- Updated docs for scenario format, artifact model, result contracts, and phase
  direction.
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
outputs/{call_id}/observations.md
```

## Still Out Of Scope

- Retell API calls.
- Real outbound calls.
- Audio recording collection.
- Retell transcript capture.
- OpenAI analysis.
- Automated pass/fail decisions.

## Next Phase

Phase 3 should add Retell outbound call integration while preserving the local
`call_id` artifact model and the conversation-quality guidance introduced here.
