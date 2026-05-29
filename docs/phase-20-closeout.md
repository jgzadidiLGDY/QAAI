# Phase 20 Closeout

Phase 20 added a deterministic local text chat runner as the first non-voice
runtime channel.

## Completed

- Added `--run-mode=text-chat`.
- Added a `TextChatRunner` that reuses scenario loading, validation, patient
  simulation prompt generation, and artifact persistence.
- Wrote text run metadata with `run_mode = text_chat` and `channel = text`.
- Wrote `transcript.txt` and normalized `transcript.json` for text chat runs.
- Left Retell ids, target phone number, audio, and manifest absent for local
  text chat runs.
- Updated artifact completeness so text runs require transcript text and JSON
  but do not warn about missing voice audio.
- Preserved existing dry-run and Retell behavior.

## Local Usage

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=text-chat"
```

Expected text run artifacts:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/transcript.txt
outputs/{call_id}/transcript.json
outputs/{call_id}/observations.md
```

## Still Out Of Scope

- External text chat providers.
- Live bidirectional chat UI.
- SMS, email, or browser-agent execution.
- AI-owned workflow control.
- AI-owned pass/fail decisions.
- Broad artifact migrations or `call_id` renames.
- Retell behavior changes.

## Tests

```powershell
.\gradlew test --tests "*TextChatRunnerTest" --tests "*ScenarioRunnerCommandTest" --tests "*ArtifactCompletenessCheckerTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Local text chat runner:
  - Base commit: `57f255a`
  - Fix commit: `0a38413`

This is a strong Phase 20 Silver candidate because it has deterministic
fail-to-pass behavior across CLI routing, scenario validation, metadata,
normalized transcript artifacts, channel-aware completeness, and tests.
