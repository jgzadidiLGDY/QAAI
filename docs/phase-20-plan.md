# Phase 20 Plan: Text Chat Runner Prototype

## Summary

Phase 20 adds a deterministic local text chat runner as the first non-voice
execution channel.

The implementation should prove the Phase 19 channel boundary without adding an
external chat provider, UI, webhook, or AI-owned conversation control.

## Goal

Run an existing scenario through a local text interaction channel and write an
inspectable artifact bundle that downstream review, analysis, evaluation, run
inspection, and reporting commands can reuse.

## Implementation Plan

1. Add a text chat runner behind the existing scenario command.
2. Support `--run-mode=text-chat`.
3. Load and validate scenario YAML through the existing scenario layer.
4. Build the existing patient simulation artifact from the scenario.
5. Generate a deterministic normalized text transcript from scenario steps.
6. Write text-channel artifacts under `outputs/{call_id}/`.
7. Record `run_mode = text_chat` and `channel = text`.
8. Leave Retell ids, target phone number, audio, and manifest absent for this
   local text channel.
9. Update artifact completeness so text runs require normalized transcript
   artifacts but do not warn about missing voice audio.
10. Add focused tests for runner output, CLI routing, and completeness.

## Likely Files

- `src/main/java/com/qaai/runner/TextChatRunner.java`
- `src/main/java/com/qaai/runner/ScenarioRunnerCommand.java`
- `src/main/java/com/qaai/artifacts/ArtifactWriter.java`
- `src/main/java/com/qaai/artifacts/ArtifactCompletenessChecker.java`
- `src/main/java/com/qaai/artifacts/RunMetadata.java`
- `src/test/java/com/qaai/runner/TextChatRunnerTest.java`
- `src/test/java/com/qaai/runner/ScenarioRunnerCommandTest.java`
- `src/test/java/com/qaai/artifacts/ArtifactCompletenessCheckerTest.java`
- docs and README updates

## Tests

Focused:

```powershell
.\gradlew test --tests "*TextChatRunnerTest" --tests "*ScenarioRunnerCommandTest" --tests "*ArtifactCompletenessCheckerTest"
```

Full:

```powershell
.\gradlew test
```

## Assumptions

- Text chat is local and deterministic in this phase.
- `call_id` remains the local artifact id for compatibility.
- `text-chat` is the operator-facing CLI value; `text_chat` is the persisted
  metadata value.
- Normalized text transcript turns can use the existing `patient` role.
- Downstream commands already depend on `transcript.json` and should not need a
  separate text-specific artifact model.

## Risks

- The text runner could become a duplicate dry run if it does not write
  normalized transcript JSON.
- Voice-specific completeness rules could incorrectly require audio or manifest
  artifacts for text runs.
- Over-generalizing the runner boundary could create churn unrelated to the
  prototype.

## Scope Boundaries

In scope:

- deterministic local text chat runner
- standard artifact bundle under `outputs/{call_id}/`
- normalized transcript JSON and text
- channel-aware metadata and completeness
- focused tests and docs

Out of scope:

- external chat providers
- live text UI
- SMS, email, or browser-agent execution
- AI-owned conversation control
- broad artifact migrations
- renaming `call_id`
- changing Retell behavior
