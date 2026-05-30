# v1 Review Guide

This guide gives a GitHub reviewer the shortest deterministic path through the
current QAAI workflow.

The review path uses local `text-chat` suite execution by default. It does not
place phone calls and does not require Retell or OpenAI credentials.

## 1. Verify The Build

```powershell
.\gradlew test
```

Expected result:

```text
BUILD SUCCESSFUL
```

## 2. Run The Local Smoke Suite

```powershell
.\gradlew bootRun --args="--suite=suites/receptionist-smoke.yaml"
```

This writes a suite summary under:

```text
outputs/suites/{suite_run_id}/
```

Each suite scenario also writes a normal run bundle under:

```text
outputs/{call_id}/
```

## 3. Inspect The Suite

Open or read:

```text
outputs/suites/{suite_run_id}/suite-report.md
outputs/suites/{suite_run_id}/suite-report.json
```

The suite report should list each scenario, generated `call_id`, status,
completeness, and artifact paths. It should not decide pass/fail.

## 4. Inspect One Run

Use a `call_id` from the suite report:

```powershell
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
```

Then inspect:

```text
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
outputs/{call_id}/transcript.json
outputs/{call_id}/observations.md
```

## 5. Run Optional Local Advisory Review

For deterministic offline review, set local providers:

```powershell
$env:QAAI_ANALYZER_PROVIDER="local"
$env:QAAI_EVALUATOR_PROVIDER="local"
$env:QAAI_REVIEW_PROVIDER="local"
```

Then run:

```powershell
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
.\gradlew bootRun --args="--evaluate-call --call-id=<local_call_id>"
.\gradlew bootRun --args="--multi-lens-review --call-id=<local_call_id>"
```

Expected advisory artifacts:

```text
outputs/{call_id}/analysis.md
outputs/{call_id}/evaluation.md
outputs/{call_id}/multi-lens-review.md
```

These artifacts are evidence-linked review aids. They do not own pass/fail
decisions.

## 6. Generate The Static Report

```powershell
.\gradlew bootRun --args="--generate-report"
```

Inspect:

```text
outputs/reports/{report_id}/index.html
outputs/reports/{report_id}/report.md
outputs/reports/{report_id}/report.json
```

The report summarizes existing run, analysis, evaluation, and scenario coverage
artifacts. It does not mutate run status.

## Review Boundaries

v1 intentionally does not include:

- database storage
- an interactive web dashboard
- hosted deployment
- automatic pass/fail decisions
- live batch Retell execution
- generated scenario promotion
- real patient data

The v1 persistence and review model is local artifacts plus generated static
reports.
