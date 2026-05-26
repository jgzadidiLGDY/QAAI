# Phase 8 Closeout

Phase 8 added deterministic conversation-quality review for existing local runs.

## Completed

- Added `--review-conversation --call-id=<local_call_id>`.
- Generated grounded `observations.md` from scenario guidance and transcript evidence.
- Cited transcript turns for welcome behavior, initiative, pacing, clarification, and workflow movement.
- Recorded missing transcript evidence explicitly instead of inferring quality.
- Appended run index entries after observations are refreshed.
- Refined patient simulation guidance for pauses, vague responses, pacing, and avoiding front-loaded facts.
- Added tests for transcript-backed review, missing transcript behavior, CLI call-id validation, and prompt guidance.

## Local Usage

Review conversation quality for an existing run:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

The command writes:

```text
outputs/{call_id}/observations.md
```

## Still Out Of Scope

- Automated pass/fail decisions.
- Conversation-quality scoring.
- Batch review.
- Cross-run clustering.
- Web UI.
- Rewriting historical output folders in bulk.

## Tests

```powershell
.\gradlew test
```

## Silver Notes

Behavior commit:

- Base commit: `000640b`
- Fix commit: `722419b`

The candidate behavior is transcript-aware conversation-quality review that
refreshes observations without inventing evidence or taking pass/fail ownership.
