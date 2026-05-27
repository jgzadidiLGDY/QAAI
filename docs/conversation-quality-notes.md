# Conversation Quality Notes

Phase 2 kept conversation-quality iteration local and deterministic. Phase 8
adds a transcript-aware review command that refreshes observations from captured
artifacts without making pass/fail decisions.

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
- a human reviewer notes placeholder

When transcript evidence is unavailable, the artifact says so explicitly and
does not infer conversation quality.

## Review Boundary

The review command is deterministic and evidence-oriented. It may cite transcript
turns and highlight review questions, but it does not score the call, decide
pass/fail, or replace human judgment.

## Phase 13 Direction

Phase 13 should make very short or shallow calls visible before reviewers rely
on analysis output. Useful advisory signals may include:

- call duration is unusually short
- transcript has too few turns
- patient never states the scenario goal
- target side never asks a workflow-specific question
- conversation ends without a confirmation, next step, or clear blocker

These signals should be grounded in captured metadata and transcript turns. They
should guide human review rather than create automatic pass/fail decisions.

## Phase 14 Direction

Phase 14 should expand scenarios with a coverage taxonomy. Scenario additions
should identify the risk they exercise, such as missing facts, clarification,
hold/silence behavior, transfer behavior, ambiguous next steps, unavailable
information, or workflow mismatch. More scenarios are useful only when each one
adds reviewable coverage.
