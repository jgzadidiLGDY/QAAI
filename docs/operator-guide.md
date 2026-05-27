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
OPENAI_ANALYSIS_MODEL=gpt-4.1-mini
OPENAI_ANALYSIS_TIMEOUT=60s

QAAI_OUTPUTS_BASE_DIR=outputs
QAAI_APP_VERSION=0.0.1-SNAPSHOT
```

Use `QAAI_ANALYZER_PROVIDER=local` for deterministic offline analysis artifacts.
Use `disabled` when analysis should fail clearly instead of calling a provider.

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
```

Run inspection is read-only. It derives completeness from `metadata.json` and
files on disk.

## Test

Run focused tests while changing one workflow area:

```powershell
.\gradlew test --tests "*Artifact*" --tests "*ScenarioRunnerCommandTest" --tests "*AnalysisServiceTest"
```

Run the full suite before closing a shared behavior change:

```powershell
.\gradlew test
```
