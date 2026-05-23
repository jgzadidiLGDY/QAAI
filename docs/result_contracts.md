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
    "transcript_text": "outputs/call_20260523_001/transcript.txt"
  }
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
          "speaker": "agent",
          "quote": "Okay, you're all set.",
          "timestamp": "00:04:12"
        }
      ]
    }
  ]
}
```

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
- scenario snapshot
- metadata JSON
- transcript text

Later phases should extend these contracts rather than replacing them silently.
