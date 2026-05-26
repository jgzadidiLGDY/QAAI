# Phase 10 Closeout

Phase 10 made AI-assisted analysis pluggable while preserving deterministic
workflow boundaries and human-owned review.

## Completed

- Added `QAAI_ANALYZER_PROVIDER=openai|local|disabled`.
- Kept OpenAI as the default analyzer provider.
- Added a deterministic local analyzer for offline demos, tests, and artifact
  flow checks.
- Added a disabled analyzer mode that fails clearly when analysis is requested.
- Passed analyzer requests through a shared request object containing scenario,
  transcript, and prompt context.
- Recorded analyzer provider and model in `metadata.json` after successful
  analysis.
- Kept evidence validation and `human_review_required = true` enforcement inside
  `AnalysisService`, independent of provider.

## Local Usage

OpenAI-backed analysis remains the default:

```powershell
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

Deterministic local analysis:

```powershell
$env:QAAI_ANALYZER_PROVIDER="local"
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

Explicitly disabled analysis:

```powershell
$env:QAAI_ANALYZER_PROVIDER="disabled"
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

## Still Out Of Scope

- Batch analysis.
- Issue clustering across runs.
- Analyzer fallback chains.
- Automated pass/fail decisions.
- Local analyzer bug inference beyond deterministic artifact-flow checks.

## Tests

```powershell
.\gradlew test --tests "*Analysis*" --tests "*QaaiPropertiesTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Base commit: `3444632`
- Fix commit: `580438f`

The candidate behavior is analyzer provider selection:
analysis can run through OpenAI, deterministic local, or disabled modes while
recording provider/model metadata and preserving provider-independent validation.
