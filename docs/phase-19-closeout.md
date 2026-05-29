# Phase 19 Closeout

Phase 19 introduced the first channel-neutral runtime contract while preserving
existing voice behavior.

## Completed

- Added `channel` to run metadata.
- Defaulted existing `dry_run` and `retell` metadata to `channel = voice`.
- Wrote `channel = voice` for new dry-run and Retell runs.
- Included `channel` in Retell request metadata and dynamic variables.
- Added `channel` to run index entries.
- Displayed `channel` in run listing and one-run inspection output.
- Included `channel` in static report run summaries.
- Updated artifact and result contracts for the additive channel field.
- Preserved existing `call_id`, `run_mode`, Retell ids, artifact paths, and
  commands.

## Local Usage

Existing commands remain unchanged:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
.\gradlew bootRun --args="--list-runs"
.\gradlew bootRun --args="--generate-report"
```

Newly written metadata includes:

```json
{
  "run_mode": "retell",
  "channel": "voice"
}
```

## Still Out Of Scope

- Real text chat execution.
- Email-agent execution.
- Web-agent execution.
- Broad package or artifact renames.
- Artifact migrations.
- Replacing Retell.
- AI-owned workflow control.
- AI-owned pass/fail decisions.

## Tests

```powershell
.\gradlew test --tests "*DryRunRunnerTest" --tests "*RetellCallRunnerTest" --tests "*RunIndexWriterTest" --tests "*ReportGenerationServiceTest" --tests "*ScenarioRunnerCommandTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Channel metadata propagation:
  - Base commit: TBD
  - Fix commit: TBD

This is a stronger Silver candidate than the Phase 19 planning docs because it
has deterministic runtime behavior across metadata serialization, backward
compatibility, Retell request metadata, run indexing, CLI inspection, static
reporting, and tests.
