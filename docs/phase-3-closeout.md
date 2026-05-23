# Phase 3 Closeout

Phase 3 added explicit Retell outbound call-start integration while keeping the
artifact contract narrow and reviewable.

## Completed

- Added a Retell API client for `POST /v2/create-phone-call`.
- Added Retell outbound call request and response records.
- Added `--run-mode=retell` while preserving dry run as the default mode.
- Added Retell config validation before real calls are attempted.
- Added local `.env` loading so documented Retell settings work with `bootRun`.
- Sent local `call_id`, scenario id, workflow, and scenario guidance to Retell
  as call metadata and dynamic variables.
- Persisted `metadata.json` with both local `call_id` and Retell `call_id`.
- Added `observations.md` for Retell call-start review notes.
- Added mocked Retell client and runner tests.
- Updated docs for setup, artifact expectations, result contracts, and usage.

## Local Usage

Dry run remains the default:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
```

Start a real Retell outbound call:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
```

## Expected Artifacts

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/observations.md
```

`metadata.json` includes:

```json
{
  "run_mode": "retell",
  "retell_call_id": "retell-call-id-from-api"
}
```

## Still Out Of Scope

- Waiting for call completion.
- Retell webhook handling.
- Transcript capture.
- Recording download.
- Normalized transcript JSON.
- AI analysis.
- Automated pass/fail decisions.

## Tests

```powershell
.\gradlew test
```

## Next Phase

Phase 4 should fetch or import Retell call artifacts after a real call exists.
The first useful slice should normalize transcript and recording metadata under
`outputs/{call_id}/` without changing the Phase 3 call-start contract.
