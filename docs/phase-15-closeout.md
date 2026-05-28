# Phase 15 Closeout

Phase 15 added an evidence-linked evaluation layer for captured transcripts.

## Completed

- Added evaluation result contracts for safety, accuracy, empathy, policy, and
  workflow completion.
- Added deterministic local evaluation with `QAAI_EVALUATOR_PROVIDER=local`.
- Added disabled evaluation mode with `QAAI_EVALUATOR_PROVIDER=disabled`.
- Added `--evaluate-call --call-id=<local_call_id>`.
- Wrote `evaluation.json` and `evaluation.md` under `outputs/{call_id}/`.
- Updated metadata with evaluation artifact paths, provider/model, and
  reproducibility command metadata.
- Appended evaluation artifacts to `manifest.json` when present.
- Extended artifact completeness and run inspection to include evaluation
  artifacts.
- Validated scored evaluation evidence against normalized transcript turns.
- Allowed explicit insufficient-evidence dimensions without guessed scores.

## Local Usage

After a run has captured `transcript.json`:

```powershell
.\gradlew bootRun --args="--evaluate-call --call-id=<local_call_id>"
```

Expected artifacts:

```text
outputs/{call_id}/evaluation.json
outputs/{call_id}/evaluation.md
```

## Still Out Of Scope

- Authoritative pass/fail decisions.
- Clinical judgment.
- Dashboard or static report view.
- Batch/corpus evaluation summaries.
- OpenAI-backed evaluation provider.
- High-volume live-call orchestration.

## Tests

```powershell
.\gradlew test --tests "*Evaluation*" --tests "*ArtifactCompletenessCheckerTest" --tests "*ScenarioRunnerCommandTest" --tests "*RunInspectionServiceTest"
.\gradlew test
```

## Silver Notes

Behavior commits:

- Deterministic evaluation model:
  - Base commit: `9ac8061`
  - Fix commit: `8b243d5`
- Evaluate-call artifact workflow:
  - Base commit: `ef642b5`
  - Fix commit: `b2f5048`

The strongest Phase 15 candidate is the evaluate-call artifact workflow because
it validates provider output against transcript evidence and crosses CLI
routing, artifact writing, metadata, manifest/index updates, and inspection.
