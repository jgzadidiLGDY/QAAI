# Voice AI QA Agent

This project builds a small, reproducible QA platform for testing healthcare
AI agents, starting with healthcare voice agents through real outbound calls.

The immediate goal is not to build a production healthcare voice platform. The goal is to build a disciplined QA workflow that can:

- execute real outbound calls to a target healthcare voice agent
- simulate realistic patient interactions from defined scenarios
- capture complete call artifacts
- identify meaningful workflow and conversational failures
- produce structured, reproducible bug findings for human review

The first supported execution channel is voice. The long-term platform direction
is a channel-neutral scenario and artifact framework that can later support
voice, text chat, email, and web-agent testing through explicit channel
adapters.

The initial target healthcare voice agent is:

```text
+18054398008
```

For notes on the AI-native build workflow and phase closeout trail, see
[AI_native_builder_journal.md](AI_native_builder_journal.md).

Additional project docs:

- [Project Specs](docs/project_specs.md)
- [Architecture](docs/architecture.md)
- [Artifacts Model](docs/artifacts_model.md)
- [Result Contracts](docs/result_contracts.md)
- [Operator Guide](docs/operator-guide.md)
- [Run Lifecycle](docs/run-lifecycle.md)

## Working Principles

- Build in small, reviewable phases.
- Keep execution deterministic where possible.
- Store every run artifact under `outputs/`.
- Link artifacts by a generated `call_id`.
- Let AI assist with analysis, but never own pass/fail decisions.
- Prefer practical artifacts over hidden automation.
- Keep each phase locally runnable before expanding scope.
- Treat voice as the first channel implementation, not the permanent boundary of
  the product.

## Proposed Tech Stack

- **Language:** Java 21
- **Application framework:** Spring Boot
- **Build tool:** Gradle
- **Scenario format:** YAML first, JSON-compatible data model
- **Voice platform:** Retell AI for outbound call execution, agent configuration, transcripts, recordings, and call metadata
- **AI analysis:** pluggable analysis client, initially backed by OpenAI over HTTP
- **Serialization:** Jackson
- **Testing:** JUnit 5, AssertJ, Spring HTTP client test support
- **Storage:** local filesystem first under `outputs/`

Retell AI should own the low-level voice infrastructure. This project should own the QA workflow, scenarios, artifacts, and analysis.

## Phased Plan

### Phase 0: Project Skeleton

Create the Java project foundation.

Deliverables:

- Spring Boot application
- Gradle build
- config loading
- scenario model
- output directory convention
- basic tests
- `.env.example`

Expected result:

```text
The app starts locally, validates configuration, and has a clear place for scenarios, source code, tests, and artifacts.
```

### Phase 1: Deterministic Scenario Runner

Run a scenario without making a real phone call.

Deliverables:

- scenario YAML parser
- scenario validation
- generated `call_id`
- dry-run transcript artifact
- scenario snapshot artifact
- metadata artifact

Expected artifacts:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
```

Local usage:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
```

This creates a local dry-run artifact bundle. The run metadata explicitly marks:

```json
{
  "run_mode": "dry_run",
  "channel": "voice",
  "retell_call_id": null
}
```

### Phase 2: Conversation Quality Iteration

Improve local conversation realism and stability before making real calls.

Deliverables:

- scenario-owned conversation-quality guidance
- welcome behavior expectations
- patient initiative and pacing guidance
- clarification behavior
- before/after observations artifact

Expected result:

```text
Given a scenario, the deterministic dry run produces a richer transcript and observations artifact that make conversation quality reviewable.
```

Expected artifacts:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
outputs/{call_id}/observations.md
```

### Phase 3: Retell Outbound Call Integration

Place a real outbound call through Retell AI.

Deliverables:

- Retell API client
- outbound call request builder
- call id mapping between local `call_id` and Retell call id
- persisted call metadata
- mocked integration tests

Expected result:

```text
Given a scenario, the system starts a real outbound call and records enough metadata to inspect what happened.
```

Local usage:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
```

The default remains a dry run. A real outbound call is only placed when
`--run-mode=retell` is provided.

Expected artifacts:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/observations.md
```

The metadata records both the local `call_id` and the external
`retell_call_id`. Transcript and recording capture remain Phase 4 work.

### Phase 4: Artifact Capture

Collect and normalize call artifacts.

Deliverables:

- transcript fetch/import
- recording metadata capture
- audio download if available from Retell
- normalized transcript format
- complete artifact manifest

Local usage after a Retell call has been started:

```powershell
.\gradlew bootRun --args="--capture-artifacts --call-id=<local_call_id>"
```

This command loads `outputs/{call_id}/metadata.json`, uses the stored
`retell_call_id`, and fetches call details through Retell's get-call API.

Expected artifacts:

```text
outputs/{call_id}/audio.wav
outputs/{call_id}/transcript.json
outputs/{call_id}/transcript.txt
outputs/{call_id}/metadata.json
outputs/{call_id}/manifest.json
```

If Retell has not yet produced a transcript or recording URL, the command writes
the available artifacts and records the gap in `manifest.json`.

### Phase 5: Scenario-Driven Patient Simulation

Use scenarios to drive realistic patient behavior.

Deliverables:

- patient persona fields
- workflow goal fields
- allowed facts and constraints
- Retell dynamic variables or agent overrides
- conversation pacing guidance
- repeat/rephrase behavior

Current Phase 5 implementation:

- scenarios include `goal.call_reason`
- dry-run and Retell call-start flows write `patient_simulation.md`
- Retell receives `patient_name`, `call_reason`, and `patient_simulation_prompt`
- starter scenarios exist for the initial workflows below

Initial workflows:

- appointment scheduling
- appointment rescheduling
- prescription refill
- billing question
- insurance verification

### Phase 6: AI-Assisted Bug Analysis

Analyze transcripts and produce structured findings.

Deliverables:

- OpenAI-backed analysis service
- transcript summary
- expected-vs-actual workflow comparison
- evidence-linked findings
- JSON and Markdown report output

Current Phase 6 implementation:

- analyzes one captured call by local `call_id`
- requires captured `transcript.json`
- writes `analysis.json` and `analysis.md`
- updates metadata analysis artifact paths
- appends analysis entries to `manifest.json` when present
- validates every finding has transcript evidence
- requires `human_review_required = true`

Local usage after artifacts have been captured:

```powershell
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

Expected artifacts:

```text
outputs/{call_id}/analysis.json
outputs/{call_id}/analysis.md
```

Important rule:

Every bug finding must cite transcript evidence. The system must not fabricate evidence or make unsupported pass/fail decisions.

### Phase 7: Add reproducible run index and artifact completeness checks

Make runs easy to repeat and compare.

Deliverables:

- run index
- scenario snapshots
- before/after notes
- artifact completeness checks
- local run listing

Expected artifacts:

```text
outputs/index.jsonl
outputs/{call_id}/observations.md
```

Current Phase 7 implementation:

- appends lifecycle entries to `outputs/index.jsonl`
- records whether expected artifacts are complete for the run state
- treats missing Retell audio as a warning instead of a hard failure
- adds `--list-runs` for quick local inspection

Local usage:

```powershell
.\gradlew bootRun --args="--list-runs"
```

### Phase 7a: Transcript Role Clarity

Clarify normalized transcript labels before deeper conversation-quality work.

Current Phase 7a implementation:

- `[patient]` remains the Retell AI simulated patient
- `[receptionist]` is the target healthcare front desk side
- Retell `agent` transcript turns normalize to `patient`
- Retell `user` transcript turns normalize to `receptionist`

### Phase 8: Expanded Conversation Quality Iteration

Improve realism and conversational stability.

Focus areas:

- welcome behavior
- whether the patient waits or speaks first
- pacing
- initiative-taking
- confusion recovery
- avoiding over-sharing
- keeping the workflow moving

Expected artifacts:

```text
outputs/{call_id}/observations.md
docs/conversation-quality-notes.md
```

Current Phase 8 implementation:

- adds `--review-conversation --call-id=<local_call_id>`
- refreshes `observations.md` from scenario guidance and transcript evidence
- cites transcript turn numbers when captured `transcript.json` exists
- records missing transcript evidence explicitly instead of inferring quality
- keeps observations advisory and human-reviewed
- refines patient simulation guidance for vague responses, pauses, pacing, and avoiding front-loaded facts

Local usage after a run exists:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

## MVP+ Scope Expansion

The project has reached a strong enough base to move from phase-built prototype
work into an MVP+ stage. MVP+ means the tool should support repeated real QA
runs with bounded external-call failures, inspectable artifacts, a replaceable
analysis module, useful run inspection, and operator-focused documentation.

MVP+ remains intentionally local-first and human-reviewed. It does not turn the
system into a production healthcare voice platform, and it does not give AI
ownership of pass/fail decisions.

### Phase 9: Reliability and Observability Hardening

Goal:

Make real-call and analysis workflows fail predictably and produce enough
context for a human operator to understand what happened.

Planned deliverables:

- explicit timeout configuration for Retell, OpenAI, and recording downloads
- tested HTTP error handling for provider failures and malformed responses
- clearer command failure context including `call_id`, command, provider, and artifact path where relevant
- application logging for important lifecycle events without logging secrets, full prompts, or transcript bodies by default
- direct tests for external-client success and failure paths

### Phase 10: Analyzer Pluggability

Goal:

Make AI-assisted analysis a replaceable module while preserving the deterministic
workflow and evidence-validation boundary.

Current Phase 10 implementation:

- `QAAI_ANALYZER_PROVIDER=openai|local|disabled`
- OpenAI remains the default analyzer provider
- `local` produces deterministic advisory analysis artifacts without network access
- `disabled` rejects `--analyze-call` with a clear error
- `metadata.json` records analyzer provider and model after successful analysis
- all analyzer reports still pass through the same evidence and human-review validation boundary

Local deterministic analysis example:

```powershell
$env:QAAI_ANALYZER_PROVIDER="local"
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

### Phase 11: Run Inspection and Workflow UX

Goal:

Make local runs easier to inspect, filter, and recover from during real QA work.

Current Phase 11 implementation:

- `--show-run --call-id=<local_call_id>` summarizes one run from local artifacts
- `--list-runs` supports filters by scenario, status, and run mode
- running without a command prints concise local workflow help
- run inspection remains read-only and derives completeness from artifact paths
- next-step hints point operators toward capture, review, or analysis when useful

Local usage:

```powershell
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
.\gradlew bootRun --args="--list-runs --scenario=appointment_reschedule_001"
.\gradlew bootRun --args="--list-runs --status=artifacts_partially_captured"
.\gradlew bootRun --args="--list-runs --run-mode=retell"
```

### Phase 12: MVP+ Documentation and Artifact Trust

Goal:

Turn the accumulated phase history into a practical operator guide and make
artifact completeness easier to trust.

Current Phase 12 implementation:

- operator-focused README sections for setup, dry runs, real calls, capture, review, and analysis
- documented run status lifecycle
- documented artifact completeness rules and troubleshooting paths
- security and privacy notes for authorized test calls and no real patient data
- additive reproducibility metadata in newly written `metadata.json`

New docs:

- [Operator Guide](docs/operator-guide.md)
- [Run Lifecycle](docs/run-lifecycle.md)

New metadata shape:

```json
{
  "reproducibility": {
    "command": "capture-artifacts",
    "app_version": "0.0.1-SNAPSHOT",
    "git_commit": "optional"
  }
}
```

## Conversation Depth and Scenario Coverage Expansion

After Phase 12, the next product concern is whether real calls are long and rich
enough to exercise the target workflow. A very short call can still produce
valid artifacts, but it may not provide enough evidence for meaningful QA.

The next mini-scope expansion keeps the system local-first and human-reviewed:

- short call duration should become an advisory review signal, not an automatic failure
- conversation depth should be judged from transcript and metadata evidence
- scenarios should expand through a curated coverage model instead of a large unstructured YAML dump
- edge cases should be explicit about the risk they are meant to exercise

### Phase 13: Conversation Depth and Short-Call Signals

Goal:

Make short or shallow conversations visible to reviewers before deeper analysis
or scenario expansion.

Planned deliverables:

- duration and turn-count review signals from captured metadata/transcripts
- advisory observations when a call ends before the patient states the goal,
  before the target side asks workflow-specific questions, or before any next
  step or confirmation is reached
- documentation for interpreting short-call signals
- tests using deterministic transcript fixtures

Out of scope:

- automatic pass/fail decisions
- provider-specific retry automation
- judging clinical quality

### Phase 14: Scenario Coverage and Edge-Case Expansion

Goal:

Expand scenario coverage with a small, intentional scenario library that covers
core workflows and meaningful edge cases.

Planned deliverables:

- scenario coverage taxonomy for workflow breadth and edge-case depth
- curated new scenarios for missing or partial facts, clarification, transfers,
  hold/silence, ambiguous next steps, unavailable information, workflow
  mismatch, and workflow recovery
- scenario validation/tests so scenarios remain deterministic and reviewable
- docs mapping each scenario to the risk it is intended to exercise

Out of scope:

- broad scenario generation without review intent
- real patient data
- AI-owned scenario pass/fail decisions

Current Phase 14 implementation:

- scenario YAML includes reviewer-facing `coverage` metadata
- scenario validation rejects missing or unsupported coverage metadata
- scenario tests dynamically discover and validate every `scenarios/*.yaml`
- the scenario library includes curated edge cases for missing details,
  transfer/hold recovery, ambiguous insurance next steps, and referral workflow
  mismatch
- [Scenario Coverage](docs/scenario-coverage.md) maps each scenario to its
  workflow area, edge-case tags, and risk focus

## Post-MVP+ Evaluation and Reporting Direction

After Phase 14, the next product concern is not more live-call volume by itself.
The project now needs an evidence-linked evaluation layer that can review calls
across consistent dimensions while preserving human judgment.

### Phase 15: Evidence-Linked Evaluation Layer

Goal:

Add advisory evaluation infrastructure for captured and imported call
transcripts.

Planned deliverables:

- separate evaluation dimensions such as safety, accuracy, empathy, policy, and
  workflow completion
- one rubric prompt per dimension
- structured evaluation output with score, rationale, uncertainty, and transcript
  evidence
- `human_review_required = true` on every evaluation result

Current Phase 15 implementation:

- adds `--evaluate-call --call-id=<local_call_id>`
- writes `evaluation.json` and `evaluation.md`
- evaluates safety, accuracy, empathy, policy, and workflow completion
- supports `QAAI_EVALUATOR_PROVIDER=local|disabled`
- defaults to deterministic local evaluation for offline artifact flow checks
- validates scored dimension evidence against normalized transcript turns
- allows explicit insufficient-evidence dimensions without guessed scores
- updates metadata with evaluation artifact paths, provider/model, and
  reproducibility metadata
- appends evaluation artifacts to `manifest.json` when present
- keeps all evaluation scores advisory and human-reviewed

Out of scope:

- authoritative pass/fail decisions
- clinical judgment
- fabricated or unsupported scores
- dashboard UI
- high-volume live-call orchestration
- aggregate evaluation summaries across a call corpus

### Phase 16: QA Dashboard or Static Report View

Goal:

Visualize trusted artifacts from runs, analyses, evaluations, and scenario
coverage after Phase 15 has produced durable evaluation outputs.

Current Phase 16 implementation:

- adds `--generate-report`
- writes a local static report bundle under `outputs/reports/{report_id}/`
- summarizes latest run history from `outputs/index.jsonl`
- summarizes evaluation dimension averages and insufficient-evidence counts
- summarizes analysis finding severities
- includes scenario coverage metadata from `scenarios/*.yaml`
- links back to raw run artifacts when paths are available
- keeps reporting read-only over existing run artifacts

Local usage:

```powershell
.\gradlew bootRun --args="--generate-report"
```

Expected artifacts:

```text
outputs/reports/{report_id}/report.json
outputs/reports/{report_id}/report.md
outputs/reports/{report_id}/index.html
```

The report is advisory and human-reviewed. It does not mutate run metadata,
change run status, or create authoritative pass/fail decisions.

## Scenario Generation Direction

After Phases 15 and 16, the system can evaluate and report on captured runs.
The next bottleneck is scenario coverage: scenarios are still mostly manual,
even though the project now benefits from diverse, risk-focused inputs.

### Phase 17: AI-Assisted Scenario Draft Generation

Goal:

Given a short description of the agent under test, generate a reviewable draft
scenario library across workflow areas and edge-case categories.

Example agent description:

```text
medical office scheduling agent
```

Planned deliverables:

- agent-under-test description input
- coverage plan artifact
- AI-generated scenario YAML drafts
- deterministic validation of generated drafts
- generation report summarizing workflows, edge cases, risks, and validation
  results
- human review boundary before any draft becomes part of `scenarios/`

Current Phase 17 implementation:

- adds `--generate-scenarios --agent-description=<description>`
- uses `QAAI_SCENARIO_GENERATOR_PROVIDER=openai|disabled`
- defaults to OpenAI-backed scenario generation
- writes draft artifacts under `outputs/scenario-generation/{generation_id}/`
- validates every generated draft with existing scenario validation rules
- records valid and invalid drafts in the generation report
- records provider/model metadata and `human_review_required = true`
- keeps generated drafts out of the committed `scenarios/` library

Local usage:

```powershell
.\gradlew bootRun --args='--generate-scenarios --agent-description=medical-office-scheduling-agent'
```

Expected draft artifacts:

```text
outputs/scenario-generation/{generation_id}/agent-description.md
outputs/scenario-generation/{generation_id}/coverage-plan.md
outputs/scenario-generation/{generation_id}/generation-report.json
outputs/scenario-generation/{generation_id}/generation-report.md
outputs/scenario-generation/{generation_id}/drafts/*.yaml
```

Generated scenarios should remain review artifacts first. They should not be
automatically promoted into the committed scenario library, should not trigger
real calls, and should not claim complete behavioral coverage.

Out of scope:

- automatic promotion into `scenarios/`
- automatic live-call execution
- AI-owned coverage completeness claims
- AI-owned pass/fail decisions
- real patient data
- unbounded scenario generation

## Structured Multi-Lens Review Direction

After Phase 17, the system can generate draft scenarios, execute or import
runs, capture artifacts, analyze calls, evaluate dimensions, and produce static
reports. The next useful step is not a full multi-agent platform. The next step
is a bounded multi-lens review layer: several specialized advisory review passes
over the same fixed call artifacts.

### Phase 18: Structured Multi-Lens Review

Goal:

Run a small set of specialized review lenses over one captured call artifact
bundle and write inspectable findings for human review.

Planned lenses:

- safety
- consistency
- patient realism
- adversarial robustness
- workflow risk

Planned deliverables:

- explicit lens registry with stable lens identifiers
- deterministic local provider for tests and offline workflow checks
- structured JSON and Markdown review artifacts
- transcript-evidence validation for every concrete finding
- clear insufficient-evidence handling when a lens cannot judge
- metadata and manifest links to review outputs
- docs explaining the boundary between multi-lens review and multi-agent
  orchestration

Current Phase 18 implementation:

- adds `--multi-lens-review --call-id=<local_call_id>`
- uses `QAAI_REVIEW_PROVIDER=local|disabled`
- defaults to deterministic local review for offline artifact flow checks
- writes `multi-lens-review.json` and `multi-lens-review.md`
- runs safety, consistency, patient realism, adversarial robustness, and
  workflow risk lenses
- validates transcript evidence for every concrete finding
- accepts explicit insufficient-evidence lens results without guessed findings
- updates metadata, manifest, and run index entries after successful review
- keeps all lens output advisory and human-reviewed

Expected artifacts:

```text
outputs/{call_id}/multi-lens-review.json
outputs/{call_id}/multi-lens-review.md
```

Out of scope:

- autonomous agent fleets
- agents coordinating with each other dynamically
- AI-owned workflow control
- AI-owned pass/fail decisions
- mutation of scenarios, transcripts, reports, or call artifacts
- fabricated evidence

The review lenses should read existing artifacts only. They should not trigger
calls, promote scenarios, change run status, or decide whether a call passed.

Local usage after a captured transcript exists:

```powershell
.\gradlew bootRun --args="--multi-lens-review --call-id=<local_call_id>"
```

## Channel-Neutral Platform Direction

After Phase 18, the platform direction becomes broader than voice while staying
grounded in the existing deterministic workflow. The same scenario, artifact,
review, and report model should eventually support multiple interaction
channels:

- voice calls
- text chat sessions
- email threads
- web-agent tasks

The first step is not to add several channels at once. Phase 19 should introduce
a channel-neutral abstraction while preserving existing voice behavior exactly.
Voice and text should be used as the two concrete examples for naming,
contracts, and future compatibility, but Phase 19 should not add a real text
runner.

### Phase 19: Channel-Neutral Scenario Model

Goal:

Separate what scenario is being tested from how the interaction is executed.

Planned deliverables:

- document channel-neutral terminology such as interaction, turn, participant,
  channel, and external run id
- identify voice-specific names that are core-platform concepts versus Retell
  adapter details
- add or update contracts so future text execution can share scenario,
  transcript, artifact, review, and reporting infrastructure
- preserve all current Retell and dry-run voice behavior
- include tests only where documentation-driven terminology changes touch code

Current Phase 19 implementation:

- adds `channel` to newly written run metadata
- defaults existing `dry_run` and `retell` metadata to `channel = voice`
- records `channel` in run index entries
- includes `channel` in Retell request metadata and dynamic variables
- shows `channel` in run inspection, run listing, and static reports
- keeps `call_id`, `run_mode`, `retell_call_id`, artifact paths, and commands
  backward-compatible

Out of scope:

- adding a real text/chat runner
- renaming every historical artifact at once
- changing existing artifact paths in a breaking way
- broad refactors unrelated to channel boundaries

### Phase 20: Text Chat Runner Prototype

Goal:

Prove the channel-neutral model by running the same scenario framework against a
simple text-chat interaction channel.

Current implementation:

- adds `--run-mode=text-chat`
- writes `run_mode = text_chat`
- writes `channel = text`
- writes `transcript.txt` and normalized `transcript.json`
- omits Retell ids, phone-number targets, audio, and manifest artifacts for the
  local text prototype
- keeps downstream review, analysis, evaluation, inspection, and reporting
  artifact-compatible through the normalized transcript

Local usage:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=text-chat"
```

Expected artifacts:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/patient_simulation.md
outputs/{call_id}/transcript.txt
outputs/{call_id}/transcript.json
outputs/{call_id}/observations.md
```

### Current Phase 13 Implementation

Phase 13 adds advisory depth signals to conversation review. After captured
artifacts exist, run:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

The refreshed `observations.md` now reports whether duration is unknown, short,
typical, or long; the total patient and receptionist turn counts; whether the
patient stated the goal; whether the receptionist asked a workflow-specific
question; and whether the conversation reached a confirmation or next step.

The expected duration band for a typical medical appointment phone call is 1 to
4 minutes. These signals remain advisory and human-reviewed.

## Proposed Folder Structure

```text
.
|-- README.md
|-- AGENTS.md
|-- AI_native_builder_journal.md
|-- .gitignore
|-- .env.example
|-- build.gradle
|-- settings.gradle
|-- src
|   |-- main
|   |   |-- java
|   |   |   `-- com
|   |   |       `-- qaai
|   |   |           |-- QAAIApplication.java
|   |   |           |-- config
|   |   |           |-- scenario
|   |   |           |-- runner
|   |   |           |-- artifacts
|   |   |           |-- retell
|   |   |           |-- analysis
|   |   |           |-- evaluation
|   |   |           |-- reporting
|   |   |           |-- scenariogeneration
|   |   |           `-- review
|   |   `-- resources
|   |       `-- application.yml
|   `-- test
|       `-- java
|           `-- com
|               `-- qaai
|-- scenarios
|   |-- appointment-reschedule.yaml
|   |-- appointment-scheduling.yaml
|   |-- prescription-refill.yaml
|   |-- billing-question.yaml
|   `-- insurance-verification.yaml
|-- docs
|   |-- project_specs.md
|   |-- architecture.md
|   |-- artifacts_model.md
|   |-- result_contracts.md
|   |-- retell-setup.md
|   |-- scenario-format.md
|   `-- conversation-quality-notes.md
|-- outputs
|   `-- .gitkeep
|-- skills
`-- silver
```

`skills/` and `silver/` are local-only workspace folders and are ignored by Git.

## What You Need To Set Up

### Retell AI

You will need:

- a Retell AI account
- a Retell API key
- access to outbound calling
- a Retell phone number or approved outbound caller configuration
- a Retell agent that can act as the patient simulator, or permission for the app to create/configure one
- webhook configuration if we use asynchronous call status and artifact capture

Likely environment variables:

```text
RETELL_API_KEY=
RETELL_AGENT_ID=
RETELL_FROM_NUMBER=
RETELL_BASE_URL=https://api.retellai.com
TARGET_AGENT_PHONE_NUMBER=+18054398008
```

### OpenAI

You will need:

- an OpenAI API key
- permission to use a model suitable for structured transcript analysis

Likely environment variables:

```text
OPENAI_API_KEY=
QAAI_ANALYZER_PROVIDER=
QAAI_EVALUATOR_PROVIDER=
QAAI_SCENARIO_GENERATOR_PROVIDER=
QAAI_REVIEW_PROVIDER=
OPENAI_ANALYSIS_MODEL=
OPENAI_SCENARIO_GENERATION_MODEL=
```

### Local Development

You will need:

- Java 21
- Gradle, or the Gradle wrapper once added
- a local `.env` file or shell environment variables

### Phone Call Considerations

Before running real calls, confirm:

- the target number is correct
- the target system can receive test calls
- test calls are allowed by the system owner
- test scenarios avoid real patient data
- recordings and transcripts are permitted for the calls being placed
- call volume starts very low

## Near-Term Next Step

Phase 19 should introduce a channel-neutral scenario and interaction model:

```text
Keep voice working as the first channel, but clarify which concepts belong to
the reusable QA platform and which belong to the Retell voice adapter.
```

This keeps the system local-first, human-reviewed, and explicit about scenario
inputs, deterministic execution, inspectable artifacts, advisory AI output, and
human-owned review decisions.
