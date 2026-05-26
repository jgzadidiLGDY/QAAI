# Phase 9 Closeout

Phase 9 hardened provider reliability and operational visibility for MVP+ runs.

## Completed

- Added explicit timeout configuration for Retell API calls, Retell recording downloads, and OpenAI analysis.
- Classified Retell create-call, Retell get-call, recording download, and OpenAI chat-completion failures with provider and operation context.
- Rejected missing provider response bodies with clear client exceptions.
- Added command failure context that includes the command, `call_id`, and scenario path when available.
- Added lifecycle logs for dry runs, Retell call start, artifact capture, recording download, analysis, and run index appends.
- Added provider-client tests for success, HTTP failure, malformed response, prompt non-leakage, and config timeout binding.

## Local Usage

Optional timeout overrides:

```powershell
$env:RETELL_API_TIMEOUT="30s"
$env:RETELL_RECORDING_DOWNLOAD_TIMEOUT="60s"
$env:OPENAI_ANALYSIS_TIMEOUT="60s"
```

Existing commands remain unchanged.

## Still Out Of Scope

- Analyzer provider selection.
- Deterministic local analyzer.
- Batch capture or batch analysis.
- Run filtering and one-run inspection.
- Automated pass/fail decisions.
- Webhook-driven retries.

## Tests

```powershell
.\gradlew test
```

## Silver Notes

Behavior commit:

- Base commit: `9ffdc24`
- Fix commit: `5e02594`

The candidate behavior is provider reliability and observability hardening:
bounded external calls, classified provider failures, and lifecycle context
without changing artifact ownership or AI-assisted review boundaries.
