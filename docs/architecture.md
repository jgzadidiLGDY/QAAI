# Architecture

This document describes the planned system shape. It will evolve as each phase adds real behavior.

## Architectural Principles

- Scenario inputs drive execution.
- Deterministic workflow code owns orchestration.
- Artifacts are written before analysis.
- AI analysis runs after evidence exists.
- Humans own final review and pass/fail decisions.
- Retell owns low-level voice execution for the first channel; this project owns
  QA workflow, scenarios, artifacts, reviews, and reports.
- Voice is the first channel implementation, not the permanent platform
  boundary.

## Planned Flow

```text
scenario YAML
  -> scenario loader and validator
  -> patient simulation prompt builder
  -> run coordinator
  -> channel runner
  -> dry-run, Retell outbound call, or future text interaction
  -> artifact writer
  -> transcript and channel artifact capture
  -> conversation-depth review signals
  -> AI-assisted analysis
  -> evidence-linked evaluation
  -> structured multi-lens review
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

Phase 19 should clarify which runner responsibilities are channel-neutral and
which belong to channel adapters. Existing Retell execution should remain the
first concrete channel path. A later text runner should fit behind the same
high-level workflow without duplicating scenario loading, artifact writing,
review, evaluation, or reporting.

### Channel Adapters

Channel adapters execute or import interactions through a specific medium while
returning normalized artifacts to the shared QA workflow.

Initial and planned examples:

- voice via Retell outbound calls
- local dry-run transcript generation
- future text chat runner
- later email or web-agent runners

Adapters may own channel-specific details such as Retell call ids, audio
recordings, chat session ids, email thread ids, browser screenshots, or DOM
snapshots. They should not own scenario validation, final pass/fail decisions,
analysis evidence validation, or report semantics.

### Artifacts

Creates and manages files under `outputs/{call_id}/`. This component should avoid hidden state and should make artifact completeness easy to verify.

Package:

```text
com.qaai.artifacts
```

Phase 7 adds an append-only `outputs/index.jsonl` and status-aware artifact
completeness checks. The index is derived from completed artifact writes; it
does not drive workflow control.

Phase 19 should define how existing `call_id` terminology maps into a broader
interaction identifier model. The project may keep `call_id` for backward
compatibility while documenting it as the local run identifier and adding
channel-specific external identifiers as metadata.

### Retell

Wraps Retell outbound call APIs, call lookup, transcript access, recording metadata, and later webhook handling.

Package:

```text
com.qaai.retell
```

MVP+ hardening should give all Retell and recording-download HTTP calls explicit
timeouts, provider-specific error messages, and tests for failed or malformed
responses. Retell failures should be visible to the operator, but they should
not corrupt existing run artifacts.

Retell is the voice-channel adapter, not the general workflow layer. Shared
scenario, artifact, review, evaluation, and reporting behavior should remain
outside this package.

### Analysis

Runs AI-assisted review after transcripts and metadata are available. Analysis must cite transcript evidence and produce structured outputs for humans.

Package:

```text
com.qaai.analysis
```

The analysis module is a pluggable component. Workflow code depends on the
`AnalysisClient` boundary, not directly on a specific AI provider. MVP+ should
make provider selection explicit and support at least:

- an OpenAI-backed analyzer for real analysis
- a deterministic local analyzer for tests, demos, and offline workflows
- a disabled or unavailable analyzer state that fails clearly when analysis is requested

Analysis output remains advisory. The service layer should continue validating
`call_id`, `scenario_id`, `human_review_required`, and exact transcript
evidence before writing analysis artifacts.

### Evaluation

Runs rubric-specific advisory evaluation after transcript evidence exists.

Package:

```text
com.qaai.evaluation
```

The evaluation layer should be separate from bug analysis. It should evaluate
dimensions such as safety, accuracy, empathy, policy, and workflow completion
through independent rubrics. Each result should include transcript evidence,
uncertainty or insufficient-evidence handling, and
`human_review_required = true`.

Evaluation scores are review aids, not pass/fail decisions. Workflow control
must not depend on AI-generated scores.

### Reporting

Generates local static report artifacts from existing run outputs.

Package:

```text
com.qaai.reporting
```

The reporting layer reads the run index, run metadata, optional analysis and
evaluation artifacts, and scenario coverage metadata. It writes
`report.json`, `report.md`, and `index.html` under `outputs/reports/{report_id}/`.
Reports are derived views for human review; they do not mutate run metadata,
change status, or own pass/fail decisions.

### Scenario Generation

Drafts scenario libraries from an agent-under-test description.

Package:

```text
com.qaai.scenariogeneration
```

The scenario generation layer should create review artifacts under
`outputs/scenario-generation/{generation_id}/`. It may use AI to draft a
coverage plan and scenario YAML files, but deterministic validation should check
the generated drafts before they are considered usable. Drafts should not be
promoted into `scenarios/` automatically and should not trigger live calls.

### Multi-Lens Review

Runs several specialized advisory review passes over one completed call artifact
bundle.

Planned package:

```text
com.qaai.review
```

The multi-lens review layer should use fixed inputs: scenario snapshot,
metadata, normalized transcript, and existing analysis/evaluation artifacts when
available. Each lens should have a stable identifier, a focused rubric, and
structured output. Concrete findings must cite transcript evidence or be marked
as insufficient evidence.

This layer is intentionally not autonomous multi-agent orchestration. Lenses
should not coordinate dynamically, trigger calls, mutate artifacts, promote
scenarios, or own pass/fail decisions. Deterministic workflow code should invoke
the configured lenses in an explicit order and write review artifacts for human
inspection.

### Config

Owns typed application settings, environment variable binding, and local defaults that are safe for tests.

Package:

```text
com.qaai.config
```

MVP+ configuration should include safe defaults for local runs and explicit
environment-backed settings for provider credentials, analyzer selection,
models, and HTTP timeout values. Secrets must remain outside committed files.

### Observability

Observability should explain what the deterministic workflow did without
turning logs into another artifact store.

MVP+ logging should cover:

- command start and completion
- scenario load and validation
- Retell call request and returned external call id
- artifact capture start, completion, and partial-capture reasons
- analysis start, completion, provider, and model
- run inspection and artifact completeness warnings

Logs should include identifiers such as `call_id`, `scenario_id`, status, and
artifact paths where useful. Logs should not include API keys, full prompts,
full transcript text, or audio contents by default.

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

Phase 8 adds deterministic conversation-quality review for existing runs. It
refreshes `observations.md` from scenario guidance and available transcript
evidence while keeping review advisory.

Phase 9 hardens reliability and observability. It should add bounded external
HTTP behavior, provider failure context, and lifecycle logging before the
workflow surface grows further.

Phase 10 makes analysis provider selection explicit and keeps the analyzer
module replaceable behind the existing client boundary.

Phase 11 improves run inspection and workflow UX through focused CLI commands,
status-aware command validation, and clearer help output.

Phase 12 consolidates MVP+ documentation and artifact trust by updating
operator docs, status lifecycle documentation, artifact completeness guidance,
and reproducibility metadata.

Phase 13 should add conversation-depth and short-call review signals. The
signals should read captured metadata and normalized transcripts, then surface
advisory observations such as low turn count, short duration, no stated goal, no
workflow-specific target response, or no confirmation/next step. They should not
score the call or decide pass/fail.

Phase 14 expands scenarios through a curated coverage model. Scenario growth is
tied to workflow breadth and explicit edge-case risks rather than volume alone.
Scenario YAML now includes reviewer-facing coverage metadata, validation
enforces supported edge-case tags, and scenario tests dynamically validate every
scenario file.

Phase 15 adds an evidence-linked evaluation layer. It creates
dimension-specific advisory outputs for safety, accuracy, empathy, policy, and
workflow completion, each grounded in transcript evidence or explicitly marked
as insufficient evidence. The first provider is deterministic local evaluation,
with disabled mode available for clear operator failure.

Phase 16 adds a local static report view over existing artifacts. It visualizes
call history, evaluation score summaries, bug severity distribution, and
scenario coverage without becoming workflow control state.

Phase 17 adds AI-assisted scenario draft generation. It accepts an
agent-under-test description, writes draft scenario artifacts and a coverage
plan under `outputs/`, validates the drafts deterministically, and preserves
human review before any generated scenario becomes part of the committed
scenario library.

Phase 18 added structured multi-lens review. It runs a bounded set of
specialized advisory lenses over existing call artifacts, validates transcript
evidence for concrete findings, writes JSON and Markdown review outputs, and
keeps all results human-reviewed.

Phase 19 should introduce a channel-neutral scenario and interaction model
without adding a second real channel. It should document and, where narrowly
needed, code the boundary between core platform concepts and voice-specific
adapter concepts. Existing voice commands and artifacts should remain
compatible.

Current Phase 19 implementation adds additive channel metadata to run metadata,
run index entries, Retell request metadata, inspection output, and static
reports. Existing voice behavior remains the only real runtime channel.

Phase 20 should add a small text chat runner prototype to prove the Phase 19
boundary. It should reuse the same scenario inputs, normalized transcript model,
artifact persistence, advisory reviews, and reporting surfaces where practical.
