# Result Contracts

This document defines the planned result shapes that make QA runs reproducible and reviewable.

The contracts are intentionally lightweight at this stage. They will become stricter as each phase adds behavior.

## Contract Principles

- Results must be tied to a `call_id`.
- Results must identify the scenario used.
- Results must be reproducible from stored inputs and artifacts where possible.
- AI-generated findings must cite evidence.
- Human review owns final judgment.

## Run Metadata Contract

Planned shape:

```json
{
  "call_id": "call_20260523_001",
  "scenario_id": "appointment_reschedule_001",
  "run_mode": "dry_run",
  "target_phone_number": "+18054398008",
  "retell_call_id": null,
  "started_at": "2026-05-23T10:00:00-04:00",
  "ended_at": "2026-05-23T10:00:05-04:00",
  "status": "completed",
  "artifact_paths": {
    "scenario": "outputs/call_20260523_001/scenario.yaml",
    "metadata": "outputs/call_20260523_001/metadata.json",
    "patient_simulation": "outputs/call_20260523_001/patient_simulation.md",
    "transcript_text": "outputs/call_20260523_001/transcript.txt",
    "observations_markdown": "outputs/call_20260523_001/observations.md"
  },
  "analysis": null
}
```

## Transcript Contract

Planned JSON shape:

```json
{
  "call_id": "call_20260523_001",
  "scenario_id": "appointment_reschedule_001",
  "source": "dry_run",
  "turns": [
    {
      "index": 1,
      "speaker": "patient",
      "text": "Hi, I need to reschedule my appointment.",
      "timestamp": null
    }
  ]
}
```

For Retell transcripts, normalized speaker labels are workflow-specific:
Retell `agent` turns become `patient`, and Retell `user` turns become
`receptionist`.
This reflects the project setup where Retell plays the simulated patient and
calls the target healthcare front desk system.

## Analysis Contract

Planned JSON shape:

```json
{
  "call_id": "call_20260523_001",
  "scenario_id": "appointment_reschedule_001",
  "summary": "The patient attempted to reschedule an appointment.",
  "findings": [
    {
      "title": "Agent did not confirm the new appointment time",
      "severity": "medium",
      "workflow": "appointment_rescheduling",
      "expected_behavior": "Agent confirms the new appointment date and time.",
      "actual_behavior": "Agent ended the workflow without confirming details.",
      "evidence": [
        {
          "artifact": "transcript.txt",
          "speaker": "receptionist",
          "quote": "Okay, you're all set.",
          "timestamp": "00:04:12"
        }
      ]
    }
  ]
}
```

## Evaluation Contract

Planned JSON shape:

```json
{
  "call_id": "call_20260523_001",
  "scenario_id": "appointment_reschedule_001",
  "human_review_required": true,
  "dimensions": [
    {
      "name": "empathy",
      "score": 4,
      "scale": "1-5",
      "rationale": "The receptionist acknowledged the request politely and avoided dismissive language.",
      "uncertainty": "low",
      "evidence": [
        {
          "artifact": "transcript.txt",
          "speaker": "receptionist",
          "quote": "I can help you with that.",
          "turn_index": 4
        }
      ]
    }
  ]
}
```

Each evaluation dimension should be produced by a specific rubric. Scores are
advisory. Missing or weak evidence should be represented explicitly rather than
filled in with a guessed score.

## Run Index Contract

Phase 7 writes append-only JSONL entries to:

```text
outputs/index.jsonl
```

Each line has this shape:

```json
{
  "call_id": "call_20260523_001",
  "scenario_id": "appointment_reschedule_001",
  "run_mode": "retell",
  "status": "artifacts_partially_captured",
  "retell_call_id": "retell_call_123",
  "started_at": "2026-05-23T10:00:00-04:00",
  "ended_at": "2026-05-23T10:05:00-04:00",
  "run_directory": "outputs/call_20260523_001",
  "metadata_path": "outputs/call_20260523_001/metadata.json",
  "complete": true,
  "missing_required_artifacts": [],
  "warnings": ["audio missing or unavailable"],
  "artifact_paths": {
    "scenario": "outputs/call_20260523_001/scenario.yaml",
    "metadata": "outputs/call_20260523_001/metadata.json",
    "transcript_text": "outputs/call_20260523_001/transcript.txt",
    "transcript_json": "outputs/call_20260523_001/transcript.json",
    "patient_simulation": "outputs/call_20260523_001/patient_simulation.md",
    "audio": null,
    "manifest": "outputs/call_20260523_001/manifest.json",
    "analysis_json": null,
    "analysis_markdown": null,
    "observations_markdown": "outputs/call_20260523_001/observations.md"
  }
}
```

The index does not replace `metadata.json`. It is a chronological inspection
aid, so the same `call_id` may appear in multiple entries as a run gains more
artifacts.

## Severity Guidance

Initial severity labels:

- `low`: confusing or awkward behavior that does not block the workflow
- `medium`: workflow completion is ambiguous or missing an important confirmation
- `high`: workflow fails or produces an incorrect outcome
- `critical`: behavior creates serious safety, privacy, or compliance concern

Severity is advisory until a human reviewer confirms it.

## Pass/Fail Boundary

The system may produce:

- summaries
- suspected issues
- evidence-linked findings
- severity suggestions
- reproducibility notes

The system must not produce an authoritative pass/fail decision without human review.

## Phase 1 Minimum Contract

Phase 1 should implement the first concrete subset:

- `call_id`
- `scenario_id`
- `run_mode = dry_run`
- `retell_call_id = null`
- scenario snapshot
- metadata JSON
- transcript text

Later phases should extend these contracts rather than replacing them silently.

## Phase 2 Contract Extension

Phase 2 adds:

- scenario-owned `conversation_quality` guidance
- transcript text that includes conversation-quality guidance
- `observations.md`
- `artifact_paths.observations_markdown`

## Phase 3 Contract Extension

Phase 3 adds Retell call-start metadata:

- `run_mode = retell`
- `retell_call_id` populated from the Retell create-phone-call response
- `status` prefixed with `retell_` when Retell returns a call status
- `artifact_paths.transcript_text = null` until transcript capture exists
- scenario metadata sent to Retell as stored call metadata and dynamic variables

Phase 3 does not claim the call completed successfully. It only records that
Retell accepted the outbound call creation request and returned a call id.

## Phase 4 Contract Extension

Phase 4 adds manual Retell artifact capture for an existing local `call_id`:

- loads existing `outputs/{call_id}/metadata.json`
- requires `run_mode = retell`
- requires `retell_call_id`
- fetches Retell call details with `GET /v2/get-call/{retell_call_id}`
- writes normalized `transcript.json`
- writes human-readable `transcript.txt`
- downloads `audio.wav` when Retell provides a recording URL
- writes `manifest.json`
- updates `metadata.artifact_paths`

New metadata path fields:

```json
{
  "artifact_paths": {
    "transcript_text": "outputs/call_20260523_001/transcript.txt",
    "transcript_json": "outputs/call_20260523_001/transcript.json",
    "audio": "outputs/call_20260523_001/audio.wav",
    "manifest": "outputs/call_20260523_001/manifest.json"
  }
}
```

Capture statuses:

- `artifacts_captured`: transcript turns and audio were captured
- `artifacts_partially_captured`: transcript or audio was unavailable

Phase 4 still does not produce bug findings or pass/fail judgments.

## Phase 5 Contract Extension

Phase 5 adds scenario-driven patient simulation instructions:

- scenario `goal.call_reason`
- generated `patient_simulation.md`
- metadata `artifact_paths.patient_simulation`
- Retell dynamic variables:
  - `patient_name`
  - `call_reason`
  - `patient_simulation_prompt`

The generated patient simulation prompt is deterministic and derived only from
the scenario. It guides patient behavior but does not own workflow pass/fail
judgment.

## Phase 6 Contract Extension

Phase 6 adds single-call AI-assisted analysis after transcript capture:

- command: `--analyze-call --call-id=<local_call_id>`
- requires `outputs/{call_id}/metadata.json`
- requires `outputs/{call_id}/scenario.yaml`
- requires `outputs/{call_id}/transcript.json`
- writes `outputs/{call_id}/analysis.json`
- writes `outputs/{call_id}/analysis.md`
- updates `metadata.artifact_paths.analysis_json`
- updates `metadata.artifact_paths.analysis_markdown`
- updates `manifest.json` when it already exists

Every finding must include at least one evidence reference whose quote appears
in the normalized transcript. The report must set `human_review_required` to
`true`.

Phase 6 still does not produce authoritative pass/fail decisions.

## Phase 7 Contract Extension

Phase 7 adds:

- `outputs/index.jsonl`
- an append-only index entry after successful artifact writes
- artifact completeness derived from metadata paths and files on disk
- `--list-runs` for local inspection

Completeness is status-aware:

- `completed` dry runs require scenario, metadata, transcript text, patient simulation, and observations
- Retell call-start statuses require scenario, metadata, patient simulation, and observations
- artifact capture statuses require transcript JSON/text and manifest in addition to call-start artifacts
- `analysis_completed` also requires `analysis.json` and `analysis.md`

Missing audio is a warning because recording availability depends on Retell
capture output.

## Phase 8 Contract Extension

Phase 8 adds deterministic conversation-quality review:

- command: `--review-conversation --call-id=<local_call_id>`
- requires `outputs/{call_id}/metadata.json`
- requires `outputs/{call_id}/scenario.yaml`
- reads `outputs/{call_id}/transcript.json` when present
- writes `outputs/{call_id}/observations.md`
- appends a run index entry after the observations artifact is written

The observations artifact may cite normalized transcript turn numbers for
welcome behavior, initiative, pacing, clarification, and workflow movement. When
transcript evidence is missing, the artifact must say it is unavailable.

Phase 8 does not change the run status, does not score conversation quality, and
does not produce authoritative pass/fail decisions.

## MVP+ Contract Direction

Phases 9 through 12 move the project into an MVP+ stage. Contract changes in
this stage should strengthen reliability, inspection, and reproducibility
without replacing the existing local file model.

MVP+ contract changes should be additive where possible. Existing artifacts
should remain readable unless a later phase explicitly documents a migration.

## Phase 9 Contract Direction

Phase 9 hardens reliability and observability:

- external Retell, OpenAI, and recording-download calls should have explicit timeout configuration
- provider failures should include useful provider and operation context
- command-level failures should include local context such as `call_id` when available
- important lifecycle events should be logged
- logs must not include API keys, full prompts, full transcript bodies, or audio contents by default

Expected configuration additions may include:

```text
QAAI_HTTP_CONNECT_TIMEOUT_SECONDS=
QAAI_HTTP_READ_TIMEOUT_SECONDS=
```

Exact names may change during implementation, but timeout values should be
environment-configurable and safe for local development.

## Phase 10 Contract Direction

Phase 10 makes the analyzer module explicitly pluggable:

- workflow code depends on an analyzer interface
- analyzer provider selection is environment-configurable with `QAAI_ANALYZER_PROVIDER`
- supported values are `openai`, `local`, and `disabled`
- the deterministic local analyzer is available for tests, demos, and offline workflows
- the OpenAI-backed analyzer has adapter-level tests
- analysis artifacts remain evidence-linked and human-reviewed
- `metadata.json` records analyzer provider and model after successful analysis

Configuration:

```text
QAAI_ANALYZER_PROVIDER=openai
OPENAI_ANALYSIS_MODEL=gpt-4.1-mini
```

Successful analysis extends run metadata:

```json
{
  "status": "analysis_completed",
  "analysis": {
    "provider": "local",
    "model": "deterministic-v1"
  }
}
```

The `disabled` provider does not write analysis artifacts. It fails clearly when
`--analyze-call` is requested.

## Phase 11 Contract Direction

Phase 11 improves run inspection and workflow UX:

- `--show-run --call-id=<local_call_id>` summarizes one local run
- `--list-runs` supports filters by scenario, status, and run mode
- help output makes the supported local workflow discoverable when no command is provided
- one-run inspection includes completeness, warnings, artifact paths, and next-step hints

Run inspection output is an operator convenience. It should read existing
metadata and index artifacts, then derive completeness from artifact paths on
disk rather than becoming a second source of truth.

## Phase 12 Contract Direction

Phase 12 consolidates MVP+ documentation and artifact trust:

- README should describe setup, dry-run, real-call, capture, review, analyze, and test workflows
- status lifecycle documentation should explain valid run states and command order
- artifact completeness rules should be documented with troubleshooting guidance
- security and privacy notes should reinforce authorized test calls and no real patient data
- reproducibility metadata is additive and optional for older artifacts

Newly written metadata may include:

```json
{
  "reproducibility": {
    "command": "analyze-call",
    "app_version": "0.0.1-SNAPSHOT",
    "git_commit": "optional"
  }
}
```

Analysis provider and model remain under the existing `analysis` object after
successful analysis. Reproducibility fields help operators understand artifact
provenance, but they do not drive workflow control.

## Phase 15 Contract Direction

Phase 15 adds evidence-linked evaluation infrastructure:

- supported dimensions should include safety, accuracy, empathy, policy, and
  workflow completion
- each dimension should have an explicit rubric prompt
- evaluation requires a normalized transcript
- evaluation writes `outputs/{call_id}/evaluation.json`
- evaluation writes `outputs/{call_id}/evaluation.md`
- every dimension must cite transcript evidence or state that evidence is
  insufficient
- every result must set `human_review_required = true`
- metadata should record evaluation provider and model when applicable

Evaluation must not replace bug analysis. It provides rubric-specific review
signals for humans.

Current implementation supports deterministic local evaluation and disabled
evaluation mode through `QAAI_EVALUATOR_PROVIDER=local|disabled`.

## Phase 16 Contract Direction

Phase 16 adds static report outputs over existing artifacts:

- reports should read run metadata, transcripts, analyses, evaluations, and
  scenario coverage metadata
- useful views include call history, evaluation score summaries, bug severity
  distribution, and scenario coverage
- report generation writes static JSON, Markdown, and HTML artifacts under
  `outputs/reports/{report_id}/`
- reporting must not create authoritative pass/fail decisions
- reporting must not become the source of truth for run state

Current report bundle:

```text
outputs/reports/{report_id}/report.json
outputs/reports/{report_id}/report.md
outputs/reports/{report_id}/index.html
```

`report.json` includes:

- `report_id`
- `generated_at`
- `human_review_required = true`
- run summaries
- evaluation score summaries and insufficient-evidence counts
- analysis severity counts
- scenario coverage summaries

## Phase 17 Contract Direction

Phase 17 adds AI-assisted scenario draft generation over an agent-under-test
description.

Generated outputs should live under:

```text
outputs/scenario-generation/{generation_id}/
```

Expected artifact bundle:

```text
outputs/scenario-generation/{generation_id}/agent-description.md
outputs/scenario-generation/{generation_id}/coverage-plan.md
outputs/scenario-generation/{generation_id}/generation-report.json
outputs/scenario-generation/{generation_id}/generation-report.md
outputs/scenario-generation/{generation_id}/drafts/*.yaml
```

`generation-report.json` should include:

- `generation_id`
- `generated_at`
- `agent_description`
- `provider`
- `model`
- `human_review_required = true`
- generated draft paths
- validation results for each draft
- coverage summary by workflow area and edge-case tag
- warnings for unsupported tags, missing required fields, or review concerns

Current implementation supports `QAAI_SCENARIO_GENERATOR_PROVIDER=openai|disabled`.
OpenAI generation uses `OPENAI_SCENARIO_GENERATION_MODEL` and writes provider
metadata to the report.

Scenario generation must not automatically promote drafts into `scenarios/`,
start calls, or claim pass/fail ownership. Drafts become canonical scenarios
only after human review and a separate explicit promotion step.

## Phase 18 Contract Direction

Phase 18 should add structured multi-lens review over existing call artifacts.

Expected command:

```powershell
.\gradlew bootRun --args="--multi-lens-review --call-id=<local_call_id>"
```

Expected artifact bundle:

```text
outputs/{call_id}/multi-lens-review.json
outputs/{call_id}/multi-lens-review.md
```

`multi-lens-review.json` should include:

- `call_id`
- `review_id`
- `generated_at`
- `provider`
- `model`
- `human_review_required = true`
- one result per configured lens
- lens id, lens label, status, summary, findings, and warnings
- transcript evidence references for concrete findings
- explicit insufficient-evidence status when the transcript cannot support a
  lens judgment

Initial lens ids:

- `safety`
- `consistency`
- `patient_realism`
- `adversarial_robustness`
- `workflow_risk`

Validation rules:

- every concrete finding must cite existing transcript evidence
- cited transcript turns must exist in `transcript.json`
- evidence text must match the cited transcript turn
- every output must set `human_review_required = true`
- lens output must remain advisory and must not contain authoritative pass/fail
  ownership

The first implementation should include a deterministic local provider so the
workflow can be tested without network access. An AI-backed provider can follow
after the artifact contract and validation boundary are stable.

Current implementation supports `QAAI_REVIEW_PROVIDER=local|disabled`.
Successful review updates `metadata.artifact_paths.multi_lens_review_json`,
`metadata.artifact_paths.multi_lens_review_markdown`, `manifest.json`, and the
run index. The `disabled` provider does not write review artifacts and fails
clearly when review is requested.
