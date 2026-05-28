# Phase 17 Closeout

Phase 17 added AI-assisted scenario draft generation.

## Completed

- Added `--generate-scenarios --agent-description=<description>`.
- Added `com.qaai.scenariogeneration`.
- Added OpenAI-backed scenario generation.
- Added disabled scenario generation mode.
- Added scenario generation provider configuration.
- Wrote draft generation artifacts under `outputs/scenario-generation/{generation_id}/`.
- Wrote `agent-description.md`, `coverage-plan.md`, `generation-report.json`,
  `generation-report.md`, and `drafts/*.yaml`.
- Validated generated draft scenarios with existing scenario validation rules.
- Recorded validation failures in the generation report instead of hiding them.
- Recorded provider/model metadata and `human_review_required = true`.
- Preserved the human review boundary before any generated scenario enters
  `scenarios/`.

## Local Usage

```powershell
.\gradlew bootRun --args='--generate-scenarios --agent-description=medical-office-scheduling-agent'
```

Expected artifacts:

```text
outputs/scenario-generation/{generation_id}/agent-description.md
outputs/scenario-generation/{generation_id}/coverage-plan.md
outputs/scenario-generation/{generation_id}/generation-report.json
outputs/scenario-generation/{generation_id}/generation-report.md
outputs/scenario-generation/{generation_id}/drafts/*.yaml
```

## Configuration

```text
QAAI_SCENARIO_GENERATOR_PROVIDER=openai
OPENAI_SCENARIO_GENERATION_MODEL=gpt-4.1-mini
OPENAI_SCENARIO_GENERATION_TIMEOUT=60s
```

Use `QAAI_SCENARIO_GENERATOR_PROVIDER=disabled` when scenario generation should
fail clearly.

## Still Out Of Scope

- Automatic promotion into `scenarios/`.
- Automatic live-call execution.
- AI-owned coverage completeness claims.
- AI-owned pass/fail decisions.
- Real patient data.
- Unbounded scenario generation.

## Tests

```powershell
.\gradlew test --tests "*ScenarioGeneration*"
.\gradlew test --tests "*ScenarioRunnerCommandTest" --tests "*QaaiPropertiesTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Scenario draft generation workflow:
  - Base commit: `49f660f`
  - Fix commit: `b0e49f8`

The strongest Phase 17 candidate is the scenario draft generation workflow
because it crosses CLI routing, provider configuration, OpenAI response parsing,
YAML artifact writing, deterministic scenario validation, and review-boundary
reporting.
