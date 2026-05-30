# Operator Guide

This guide describes the local MVP+ workflow for authorized QA calls. It is
operator-focused: run one command, inspect the artifacts, then move to the next
deterministic step.

Do not use real patient data. Use only authorized test calls, synthetic
scenario facts, and approved target numbers.

## Setup

Create `.env` from `.env.example` or set equivalent shell variables:

```text
RETELL_API_KEY=
RETELL_AGENT_ID=
RETELL_FROM_NUMBER=
RETELL_BASE_URL=https://api.retellai.com
RETELL_API_TIMEOUT=30s
RETELL_RECORDING_DOWNLOAD_TIMEOUT=60s
TARGET_AGENT_PHONE_NUMBER=+18054398008

OPENAI_API_KEY=
QAAI_ANALYZER_PROVIDER=openai
QAAI_EVALUATOR_PROVIDER=local
QAAI_SCENARIO_GENERATOR_PROVIDER=openai
QAAI_REVIEW_PROVIDER=local
OPENAI_ANALYSIS_MODEL=gpt-4.1-mini
OPENAI_ANALYSIS_TIMEOUT=60s
OPENAI_SCENARIO_GENERATION_MODEL=gpt-4.1-mini
OPENAI_SCENARIO_GENERATION_TIMEOUT=60s

QAAI_OUTPUTS_BASE_DIR=outputs
QAAI_APP_VERSION=0.0.1-SNAPSHOT
```

Use `QAAI_ANALYZER_PROVIDER=local` for deterministic offline analysis artifacts.
Use `disabled` when analysis should fail clearly instead of calling a provider.
Use `QAAI_EVALUATOR_PROVIDER=local` for deterministic evidence-linked
evaluation artifacts, or `disabled` when evaluation should fail clearly.
Use `QAAI_SCENARIO_GENERATOR_PROVIDER=openai` for AI-assisted scenario drafting,
or `disabled` when draft generation should fail clearly.
Use `QAAI_REVIEW_PROVIDER=local` for deterministic multi-lens review artifacts,
or `disabled` when multi-lens review should fail clearly.

For an offline local review pass, set:

```text
QAAI_ANALYZER_PROVIDER=local
QAAI_EVALUATOR_PROVIDER=local
QAAI_REVIEW_PROVIDER=local
```

Scenario generation still requires OpenAI unless
`QAAI_SCENARIO_GENERATOR_PROVIDER=disabled` is selected.

## Dry Run

Dry runs do not place phone calls. They validate the scenario path and write a
local artifact bundle:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
```

Inspect:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/transcript.txt
outputs/{call_id}/observations.md
```

## Text Chat Run

Text chat runs do not use Retell or place phone calls. They validate the same
scenario input and write a local text-channel artifact bundle:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=text-chat"
```

Inspect:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/transcript.txt
outputs/{call_id}/transcript.json
outputs/{call_id}/observations.md
```

Text chat metadata records `run_mode = text_chat` and `channel = text`.
Retell ids, phone-number targets, audio, and manifest artifacts are absent in
this local prototype.

## Suite Run

Suite runs execute a reviewed scenario set against a named agent profile. The
first supported suite execution path is local `text-chat`, so it does not place
phone calls:

```powershell
.\gradlew bootRun --args="--suite=suites/receptionist-smoke.yaml"
```

Inspect the suite summary:

```text
outputs/suites/{suite_run_id}/suite.yaml
outputs/suites/{suite_run_id}/agent-profile.yaml
outputs/suites/{suite_run_id}/suite-report.json
outputs/suites/{suite_run_id}/suite-report.md
```

Each scenario in the suite also writes a normal run bundle under
`outputs/{call_id}/`. Run metadata records `agent_profile_id`, `suite_id`, and
`suite_run_id` so individual artifacts remain linked to the suite context.

Suite reports summarize run artifacts for human review. They do not run
analysis, evaluation, or multi-lens review automatically, and they do not decide
pass/fail.

## Real Call Start

Retell mode starts an outbound call and records the local-to-Retell id mapping:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
```

This step does not claim the call completed. Capture remains a separate command.

## Capture Artifacts

After Retell has produced call details, capture the transcript, manifest, and
audio when available:

```powershell
.\gradlew bootRun --args="--capture-artifacts --call-id=<local_call_id>"
```

Missing audio is a warning. Missing transcript data makes the run partially
captured and should be inspected before analysis.

## Review Conversation

Refresh advisory conversation-quality observations from the scenario snapshot
and any available normalized transcript:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

This command does not score the call and does not change pass/fail ownership.

## Analyze

Analyze a captured transcript:

```powershell
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

Analysis writes advisory `analysis.json` and `analysis.md`. Every finding must
cite transcript evidence and `human_review_required` remains true.

## Evaluate

Evaluate a captured transcript with advisory rubrics:

```powershell
.\gradlew bootRun --args="--evaluate-call --call-id=<local_call_id>"
```

Evaluation writes `evaluation.json` and `evaluation.md`. Scored dimensions must
cite transcript evidence; weak or missing evidence is recorded explicitly
instead of guessed. Human review remains required.

## Multi-Lens Review

Review a captured transcript through stable advisory lenses:

```powershell
.\gradlew bootRun --args="--multi-lens-review --call-id=<local_call_id>"
```

Multi-lens review writes `multi-lens-review.json` and
`multi-lens-review.md`. Concrete findings must cite transcript evidence;
insufficient evidence is recorded explicitly. The lenses do not coordinate
dynamically, mutate artifacts, or own pass/fail decisions.

## Inspect Runs

Inspect one run:

```powershell
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
```

List or filter the run index:

```powershell
.\gradlew bootRun --args="--list-runs"
.\gradlew bootRun --args="--list-runs --scenario=appointment_reschedule_001"
.\gradlew bootRun --args="--list-runs --status=artifacts_partially_captured"
.\gradlew bootRun --args="--list-runs --run-mode=retell"
.\gradlew bootRun --args="--list-runs --run-mode=text_chat"
```

Run inspection is read-only. It derives completeness from `metadata.json` and
files on disk.

## Generate Static Report

Generate a local report bundle across existing runs:

```powershell
.\gradlew bootRun --args="--generate-report"
```

Inspect:

```text
outputs/reports/{report_id}/report.json
outputs/reports/{report_id}/report.md
outputs/reports/{report_id}/index.html
```

The report reads existing metadata, analysis, evaluation, run-index, and
scenario coverage artifacts. It does not change run status and does not own
pass/fail decisions.

## Generate Scenario Drafts

Generate review-only scenario drafts from an agent-under-test description:

```powershell
.\gradlew bootRun --args='--generate-scenarios --agent-description=medical-office-scheduling-agent'
```

Inspect:

```text
outputs/scenario-generation/{generation_id}/agent-description.md
outputs/scenario-generation/{generation_id}/coverage-plan.md
outputs/scenario-generation/{generation_id}/generation-report.json
outputs/scenario-generation/{generation_id}/generation-report.md
outputs/scenario-generation/{generation_id}/drafts/*.yaml
```

Drafts are not copied into `scenarios/` automatically. Review the generation
report and each draft before promoting any scenario into actual use.

## Test

Run focused tests while changing one workflow area:

```powershell
.\gradlew test --tests "*Evaluation*" --tests "*MultiLensReview*" --tests "*ScenarioRunnerCommandTest"
```

Run the full suite before closing a shared behavior change:

```powershell
.\gradlew test
```
