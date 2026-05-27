# Phase 11 Closeout

Phase 11 made local run inspection easier during repeated QA work without
changing artifact ownership or adding hidden workflow state.

## Completed

- Added `--show-run --call-id=<local_call_id>` for one-run inspection.
- Added filtered `--list-runs` support for scenario, status, and run mode.
- Added concise help output when no command is provided.
- Kept run inspection read-only; it reads existing metadata and index entries
  and derives completeness from artifact paths on disk.
- Added next-step hints for common local workflow states, such as Retell runs
  that still need artifact capture and captured runs that still need analysis.

## Local Usage

Inspect one run:

```powershell
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
```

Filter the run index:

```powershell
.\gradlew bootRun --args="--list-runs --scenario=appointment_reschedule_001"
.\gradlew bootRun --args="--list-runs --status=artifacts_partially_captured"
.\gradlew bootRun --args="--list-runs --run-mode=retell"
```

Print help:

```powershell
.\gradlew bootRun
```

## Still Out Of Scope

- Batch capture or batch analysis.
- Latest-run shortcuts.
- Issue clustering across runs.
- New artifact persistence formats.
- Automated pass/fail decisions.

## Tests

```powershell
.\gradlew test --tests "*ScenarioRunnerCommandTest" --tests "*RunInspectionServiceTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Base commit: `79e2a0d`
- Fix commit: `38cdd1a`

The candidate behavior is local run inspection:
operators can inspect one run by `call_id`, filter the run index by scenario,
status, or run mode, and discover supported commands when no command is
provided, without turning the index into workflow control state.
