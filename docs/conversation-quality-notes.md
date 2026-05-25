# Conversation Quality Notes

Phase 2 keeps conversation-quality iteration local and deterministic.

## Current Focus

- Welcome behavior: how the patient starts with the configured Retell welcome message.
- Initiative: how much information the patient volunteers without prompting.
- Pacing: whether each turn stays short and natural.
- Clarification: how the patient responds to unclear questions.
- Expected risks: known issues a reviewer should watch for in later real calls.

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

## Later Iteration

After Retell calls and real transcripts exist, reviewers can compare actual
agent behavior against the scenario's expected risks and add grounded notes to
the observation artifact.
