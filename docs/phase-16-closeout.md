# Phase 16 Closeout

Phase 16 added a local static report view over trusted QA artifacts.

## Completed

- Added `--generate-report`.
- Added `com.qaai.reporting` for report aggregation and rendering.
- Wrote report bundles under `outputs/reports/{report_id}/`.
- Generated `report.json`, `report.md`, and `index.html`.
- Summarized latest run history from `outputs/index.jsonl`.
- Summarized evaluation scores and insufficient-evidence counts by dimension.
- Counted analysis finding severities when `analysis.json` is present.
- Included scenario coverage metadata from `scenarios/*.yaml`.
- Linked report rows back to raw artifact paths when available.
- Kept report generation read-only over existing run artifacts.

## Local Usage

```powershell
.\gradlew bootRun --args="--generate-report"
```

Expected artifacts:

```text
outputs/reports/{report_id}/report.json
outputs/reports/{report_id}/report.md
outputs/reports/{report_id}/index.html
```

## Still Out Of Scope

- Interactive dashboard server.
- Report-driven workflow control.
- Authoritative pass/fail decisions.
- AI-generated aggregate conclusions.
- Mutating historical run metadata.
- High-volume live-call orchestration.

## Tests

```powershell
.\gradlew test --tests "*Report*"
.\gradlew test --tests "*ScenarioRunnerCommandTest"
.\gradlew test
```

## Silver Notes

Behavior commit:

- Static QA report generation:
  - Base commit: `ad0955d`
  - Fix commit: `904c3c6`

The strongest Phase 16 candidate is static QA report generation because it
connects CLI routing, index reading, metadata loading, analysis/evaluation
parsing, scenario coverage parsing, and deterministic report artifact writing.
