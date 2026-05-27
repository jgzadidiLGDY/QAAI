# Conversation Quality Notes

Phase 2 kept conversation-quality iteration local and deterministic. Phase 8
added a transcript-aware review command that refreshes observations from
captured artifacts without making pass/fail decisions. Phase 13 adds advisory
conversation-depth signals so short or shallow calls are visible before deeper
analysis.

## Current Focus

- Welcome behavior: how the patient starts with the configured Retell welcome message.
- Initiative: how much information the patient volunteers without prompting.
- Pacing: whether each turn stays short and natural.
- Clarification: how the patient responds to unclear questions.
- Expected risks: known issues a reviewer should watch for in later real calls.
- Conversation depth: whether the call lasted long enough and included enough
  workflow movement to support meaningful QA review.

## Scenario Ownership

Conversation-quality guidance lives in each scenario under `conversation_quality`.
The dry-run runner reads those fields and writes them into the transcript and
observations artifact. This keeps the guidance reproducible from the scenario
snapshot stored under `outputs/{call_id}/scenario.yaml`.

## Artifact Expectations

Phase 2 dry runs produce:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
outputs/{call_id}/observations.md
```

`observations.md` is a deterministic starting point for human review. It does
not score the call, decide pass/fail, or invent evidence.

## Phase 8 Review Command

Run conversation-quality review for an existing local run:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

The command reads:

- `outputs/{call_id}/metadata.json`
- `outputs/{call_id}/scenario.yaml`
- `outputs/{call_id}/transcript.json` when available

It refreshes:

```text
outputs/{call_id}/observations.md
```

The generated observations include:

- scenario conversation-quality guidance
- welcome behavior evidence
- initiative and over-sharing prompts for review
- pacing evidence
- clarification and confusion-recovery evidence
- workflow movement evidence
- duration, turn-count, goal-statement, workflow-question, and confirmation or
  next-step depth signals
- a human reviewer notes placeholder

When transcript evidence is unavailable, the artifact says so explicitly and
does not infer conversation quality.

## Phase 13 Depth Signals

The review command now adds a `Conversation Depth Signals` section to
`observations.md`.

Duration is interpreted as:

- `unknown`: no Retell duration and no transcript timestamps are available.
- `short`: less than 60 seconds.
- `typical`: 60 to 240 seconds, matching the expected 1 to 4 minute range for a
  typical medical appointment phone call.
- `long`: more than 240 seconds.

Depth signals also summarize:

- total, patient, and receptionist turn counts
- whether the patient stated the scenario goal
- whether the receptionist asked a workflow-specific question
- whether the conversation reached confirmation, a next step, or a clear
  workflow movement cue

These checks are deterministic heuristics. They are meant to point the reviewer
toward evidence, not to score the call.

Live Phase 13 calibration:

```text
call_id: call_20260527_133343_cd7d9a47
retell_call_id: call_cc84b5fabee359005c74e4f6b65
call_duration_seconds: 131
turn_count: 27 total, 13 patient, 14 receptionist
duration_signal: typical
```

## Review Boundary

The review command is deterministic and evidence-oriented. It may cite transcript
turns and highlight review questions, but it does not score the call, decide
pass/fail, or replace human judgment.

## Phase 13 Direction

Phase 13 makes very short or shallow calls visible before reviewers rely on
analysis output. Advisory signals include:

- call duration is unusually short
- transcript has too few turns
- patient never states the scenario goal
- target side never asks a workflow-specific question
- conversation ends without a confirmation, next step, or clear blocker

These signals are grounded in captured metadata and transcript turns. They guide
human review rather than create automatic pass/fail decisions.

## Phase 14 Direction

Phase 14 should expand scenarios with a coverage taxonomy. Scenario additions
should identify the risk they exercise, such as missing facts, clarification,
hold/silence behavior, transfer behavior, ambiguous next steps, unavailable
information, or workflow mismatch. More scenarios are useful only when each one
adds reviewable coverage.
