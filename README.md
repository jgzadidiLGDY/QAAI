# Voice AI QA Agent

This project builds a small, reproducible QA platform for testing healthcare voice agents through real outbound calls.

The immediate goal is not to build a production healthcare voice platform. The goal is to build a disciplined QA workflow that can:

- execute real outbound calls to a target healthcare voice agent
- simulate realistic patient interactions from defined scenarios
- capture complete call artifacts
- identify meaningful workflow and conversational failures
- produce structured, reproducible bug findings for human review

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

## Working Principles

- Build in small, reviewable phases.
- Keep execution deterministic where possible.
- Store every run artifact under `outputs/`.
- Link artifacts by a generated `call_id`.
- Let AI assist with analysis, but never own pass/fail decisions.
- Prefer practical artifacts over hidden automation.
- Keep each phase locally runnable before expanding scope.

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

Planned deliverables:

- `--show-run --call-id=<local_call_id>` for one-run inspection
- filtered run listing by scenario, status, or run mode
- clearer command validation when capture, review, or analysis is requested too early
- concise help output when no command is provided
- optional convenience affordances for local iteration, such as latest-run inspection

### Phase 12: MVP+ Documentation and Artifact Trust

Goal:

Turn the accumulated phase history into a practical operator guide and make
artifact completeness easier to trust.

Planned deliverables:

- operator-focused README sections for setup, dry runs, real calls, capture, review, and analysis
- documented run status lifecycle
- documented artifact completeness rules and troubleshooting paths
- security and privacy notes for authorized test calls and no real patient data
- metadata refinements for reproducibility, such as command context, analyzer provider/model, and app or git version where practical

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
|   |   |           `-- analysis
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
OPENAI_ANALYSIS_MODEL=
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

Phase 11 should make repeated local QA work easier to inspect and recover from:

```text
Add one-run inspection, useful run filters, clearer command validation, and
concise help output for the local workflow.
```

This should build on the existing artifact capture, analysis, run index,
conversation-quality review, and analyzer selection behavior without changing
the human-owned pass/fail boundary.
