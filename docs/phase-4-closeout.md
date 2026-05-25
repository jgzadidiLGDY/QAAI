# Phase 4 Closeout

Phase 4 added manual, deterministic artifact capture for existing Retell calls.

## Completed

- Added `--capture-artifacts --call-id=<local_call_id>`.
- Loaded existing run metadata from `outputs/{call_id}/metadata.json`.
- Required `run_mode = retell` and a stored `retell_call_id`.
- Added Retell `GET /v2/get-call/{call_id}` support.
- Added normalized `transcript.json`.
- Added human-readable `transcript.txt`.
- Added recording download to `audio.wav` when Retell provides a recording URL.
- Added `manifest.json` for artifact inventory and missing-artifact notes.
- Updated `metadata.json` with transcript, audio, and manifest paths.
- Added tests for Retell call lookup and artifact capture behavior.
- Updated docs for usage, artifact expectations, and result contracts.

## Local Usage

Start a Retell call:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
```

Capture artifacts for the local `call_id` printed by that run:

```powershell
.\gradlew bootRun --args="--capture-artifacts --call-id=<local_call_id>"
```

## Expected Artifacts

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.json
outputs/{call_id}/transcript.txt
outputs/{call_id}/audio.wav
outputs/{call_id}/manifest.json
outputs/{call_id}/observations.md
```

`audio.wav` is present only when Retell provides a recording URL and the
download succeeds.

## Still Out Of Scope

- Webhook handling.
- Poll-until-complete behavior.
- AI-assisted bug analysis.
- Automated pass/fail decisions.
- Batch artifact capture.
- Run indexing.

## Tests

```powershell
.\gradlew test
```

## Next Phase

Phase 5 should begin scenario-driven patient simulation improvements, using the
artifact capture contract from Phase 4 as the evidence base for future analysis.
