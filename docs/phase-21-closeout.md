# Phase 21 Closeout

Phase 21 made the agent under test and deterministic scenario suites first-class
platform concepts.

## Completed

- Added `agent-profiles/medical-receptionist-demo.yaml`.
- Added `suites/receptionist-smoke.yaml`.
- Added agent profile loading and validation.
- Added scenario suite loading and validation.
- Added `--suite=<path>` command routing.
- Added deterministic suite execution through local `text-chat` runs.
- Extended run metadata and run index entries with `agent_profile_id`,
  `suite_id`, and `suite_run_id`.
- Wrote suite artifacts under `outputs/suites/{suite_run_id}/`.
- Preserved ordinary per-scenario run bundles under `outputs/{call_id}/`.
- Kept downstream analysis, evaluation, and multi-lens review explicit per
  `call_id`.

## Local Usage

```powershell
.\gradlew bootRun --args="--suite=suites/receptionist-smoke.yaml"
```

Expected suite artifacts:

```text
outputs/suites/{suite_run_id}/suite.yaml
outputs/suites/{suite_run_id}/agent-profile.yaml
outputs/suites/{suite_run_id}/suite-report.json
outputs/suites/{suite_run_id}/suite-report.md
```

Each suite scenario also writes a normal text chat run bundle under
`outputs/{call_id}/`.

## Still Out Of Scope

- Database storage.
- Web dashboard changes.
- Automatic pass/fail decisions.
- Automatic downstream analysis, evaluation, or multi-lens review.
- Live batch Retell execution by default.
- Generated scenario promotion.
- Broad artifact migration or `call_id` renaming.

## Tests

```powershell
.\gradlew test --tests "*AgentProfileLoaderTest" --tests "*ScenarioSuiteLoaderTest" --tests "*SuiteRunServiceTest" --tests "*ScenarioRunnerCommandTest" --tests "*TextChatRunnerTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Agent profiles and deterministic suite runs:
  - Base commit: `8264e9e`
  - Fix commit: `95bc042`

This should be a stronger Silver candidate than the Phase 21 planning commit
because it has deterministic fail-to-pass behavior across YAML validation, CLI
routing, metadata propagation, run index entries, suite artifact generation, and
focused tests.
