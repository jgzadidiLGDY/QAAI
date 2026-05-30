# Voice AI QA Agent

Voice AI QA Agent is a local-first QA workflow for testing healthcare AI agents
with scenario-driven patient behavior, reproducible artifacts, and
human-reviewed advisory analysis.

The project is not a production healthcare voice platform and does not make
clinical or pass/fail decisions. It helps a reviewer run synthetic scenarios,
inspect transcripts and artifacts, and identify likely workflow or
conversation-quality issues from evidence.

## v1 Status

The current implementation is the v1.0.0 release candidate. It supports:

- deterministic scenario runs
- local text-chat runs
- Retell outbound call start and artifact capture
- normalized transcript and metadata artifacts under `outputs/`
- run inspection and append-only run indexing
- advisory conversation review, analysis, evaluation, and multi-lens review
- AI-assisted scenario draft generation for human review
- agent-under-test profiles and deterministic suite runs
- static JSON, Markdown, and HTML reports over existing artifacts

The v1 review surface is intentionally local: CLI commands plus inspectable
files. A database and interactive web dashboard are out of scope for v1.

## Quick Start

Prerequisites:

- Java 21
- the Gradle wrapper included in this repository

Run tests:

```powershell
.\gradlew test
```

Run the deterministic local smoke suite:

```powershell
.\gradlew bootRun --args="--suite=suites/receptionist-smoke.yaml"
```

Inspect generated artifacts:

```text
outputs/suites/{suite_run_id}/suite-report.md
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
outputs/{call_id}/transcript.json
```

Generate a static report from local run history:

```powershell
.\gradlew bootRun --args="--generate-report"
```

Open or inspect:

```text
outputs/reports/{report_id}/index.html
outputs/reports/{report_id}/report.md
outputs/reports/{report_id}/report.json
```

For a step-by-step reviewer path, see
[v1 Review Guide](docs/v1-review-guide.md).

## Configuration

Copy `.env.example` to `.env` or set equivalent environment variables.

For deterministic offline review, use local providers:

```text
QAAI_ANALYZER_PROVIDER=local
QAAI_EVALUATOR_PROVIDER=local
QAAI_REVIEW_PROVIDER=local
QAAI_SCENARIO_GENERATOR_PROVIDER=disabled
```

OpenAI is only required for OpenAI-backed analysis or scenario draft
generation. Retell credentials are only required for real outbound voice calls
and artifact capture.

Do not use real patient data. Use only authorized test calls, synthetic
scenario facts, and approved target numbers.

## Core Commands

Run one local dry run:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
```

Run one local text-channel interaction:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=text-chat"
```

Start a real Retell outbound call:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
```

Capture Retell artifacts after call details are available:

```powershell
.\gradlew bootRun --args="--capture-artifacts --call-id=<local_call_id>"
```

Run advisory review steps over an existing transcript:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
.\gradlew bootRun --args="--evaluate-call --call-id=<local_call_id>"
.\gradlew bootRun --args="--multi-lens-review --call-id=<local_call_id>"
```

Inspect run history:

```powershell
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
.\gradlew bootRun --args="--list-runs"
```

## Artifact Model

Runtime artifacts are written under `outputs/`, which is ignored by Git.

Per-run artifacts use the local `call_id`:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/transcript.txt
outputs/{call_id}/transcript.json
outputs/{call_id}/observations.md
outputs/{call_id}/analysis.md
outputs/{call_id}/evaluation.md
outputs/{call_id}/multi-lens-review.md
```

Suite and report artifacts use:

```text
outputs/suites/{suite_run_id}/
outputs/reports/{report_id}/
```

The artifact store is the v1 persistence layer. Reports and indexes summarize
existing artifacts; they do not become workflow control state.

## Project Docs

- [v1 Review Guide](docs/v1-review-guide.md)
- [v1 Release Notes](docs/v1-release-notes.md)
- [Operator Guide](docs/operator-guide.md)
- [Project Specs](docs/project_specs.md)
- [Architecture](docs/architecture.md)
- [Artifacts Model](docs/artifacts_model.md)
- [Result Contracts](docs/result_contracts.md)
- [Run Lifecycle](docs/run-lifecycle.md)
- [Scenario Format](docs/scenario-format.md)
- [Scenario Coverage](docs/scenario-coverage.md)

## Build History

The detailed phase history lives outside this README so the repository front
page stays reviewable:

- [AI-Native Building Notes](AI_native_builder_journal.md)
- [Phase 22 Plan](docs/phase-22-plan.md)
- [Phase 21 Closeout](docs/phase-21-closeout.md)
- earlier phase closeouts linked from the build journal

## Tech Stack

- Java 21
- Spring Boot
- Gradle
- YAML scenario inputs
- Retell for outbound voice execution and call artifacts
- OpenAI-backed or deterministic local advisory analysis
- Jackson serialization
- JUnit 5 and AssertJ tests
- local filesystem artifacts under `outputs/`

## Working Principles

- Scenario inputs drive execution.
- Deterministic workflow code owns orchestration.
- Artifacts are written before analysis.
- AI assists with summaries, findings, drafts, and reports.
- Humans own final review and pass/fail decisions.
- No fabricated evidence.
- No real patient data.
