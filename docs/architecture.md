# Architecture

This document describes the planned system shape. It will evolve as each phase adds real behavior.

## Architectural Principles

- Scenario inputs drive execution.
- Deterministic workflow code owns orchestration.
- Artifacts are written before analysis.
- AI analysis runs after evidence exists.
- Humans own final review and pass/fail decisions.
- Retell owns low-level voice execution; this project owns QA workflow and artifacts.

## Planned Flow

```text
scenario YAML
  -> scenario loader and validator
  -> patient simulation prompt builder
  -> run coordinator
  -> dry-run or Retell outbound call
  -> artifact writer
  -> transcript and recording capture
  -> AI-assisted analysis
  -> human-reviewable report
```

## Main Components

### Scenario

Owns patient scenario definitions, expected outcomes, allowed facts, and workflow constraints.

Package:

```text
com.qaai.scenario
```

Phase 5 adds `PatientSimulationPromptBuilder`, which converts a scenario into a
deterministic `patient_simulation_prompt` for Retell and the local
`patient_simulation.md` artifact.

### Runner

Coordinates a QA run from scenario input to output artifact bundle. The runner should keep execution order explicit and inspectable.

Package:

```text
com.qaai.runner
```

### Artifacts

Creates and manages files under `outputs/{call_id}/`. This component should avoid hidden state and should make artifact completeness easy to verify.

Package:

```text
com.qaai.artifacts
```

Phase 7 adds an append-only `outputs/index.jsonl` and status-aware artifact
completeness checks. The index is derived from completed artifact writes; it
does not drive workflow control.

### Retell

Wraps Retell outbound call APIs, call lookup, transcript access, recording metadata, and later webhook handling.

Package:

```text
com.qaai.retell
```

### Analysis

Runs AI-assisted review after transcripts and metadata are available. Analysis must cite transcript evidence and produce structured outputs for humans.

Package:

```text
com.qaai.analysis
```

### Config

Owns typed application settings, environment variable binding, and local defaults that are safe for tests.

Package:

```text
com.qaai.config
```

## Storage Model

Local filesystem storage is the first persistence layer:

```text
outputs/{call_id}/
```

This keeps the early system simple, inspectable, and easy to reset. A database can be added later if run history, indexing, or search require it.

## External Systems

### Retell AI

Retell provides:

- outbound call execution
- voice agent behavior
- transcripts
- recordings
- call metadata

### OpenAI

OpenAI provides:

- transcript summarization
- issue extraction
- evidence-linked report generation

OpenAI is not used for core workflow control.

## Phase Boundaries

Phase 0 created the runnable Java/Spring Boot foundation.

Phase 1 added deterministic scenario loading and dry-run artifacts.

Phase 2 added scenario-owned conversation-quality guidance and a deterministic
observations artifact.

Phase 3 added explicit Retell call-start execution. The runner can now choose
between `dry-run` and `retell` modes, and Retell mode persists the mapping
between the local `call_id` and Retell's `call_id`.

Phase 4 adds a manual artifact capture command for an existing local `call_id`.
It fetches Retell call details, normalizes transcripts, downloads audio when
available, and writes a manifest under `outputs/{call_id}/`.

Webhook handling and AI analysis belong to later phases.

Phase 5 adds scenario-driven patient simulation instructions. Dry-run and Retell
call-start flows now write `patient_simulation.md`, and Retell receives
`patient_name`, `call_reason`, and `patient_simulation_prompt` dynamic
variables.

Phase 6 adds single-call AI-assisted analysis after transcript capture.
Analysis writes evidence-linked JSON and Markdown reports, requires human
review, and rejects unsupported transcript evidence.

Phase 7 adds reproducible run indexing and artifact completeness checks. Each
successful artifact write appends a JSONL entry under `outputs/index.jsonl`, and
`--list-runs` prints the local run history for inspection.
