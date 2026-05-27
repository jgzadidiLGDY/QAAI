# Run Lifecycle

The local workflow is intentionally explicit. Each command writes or inspects
artifacts under `outputs/{call_id}/`, and humans decide final QA outcomes.

## Command Order

```text
dry run:
  --scenario=<path>

real call:
  --scenario=<path> --run-mode=retell
  --capture-artifacts --call-id=<local_call_id>
  --review-conversation --call-id=<local_call_id>
  --analyze-call --call-id=<local_call_id>
```

`--show-run` and `--list-runs` can be used at any point after artifacts exist.

## Statuses

| Status | Meaning | Typical next step |
| --- | --- | --- |
| `completed` | Dry-run artifacts were written. | Inspect artifacts or start a Retell run. |
| `retell_*` | Retell accepted the outbound call request and returned a call id. | Capture artifacts after the call has produced details. |
| `started` | Retell accepted the call but returned no specific call status. | Capture artifacts after the call has produced details. |
| `artifacts_captured` | Transcript artifacts and audio were captured. | Review conversation or analyze transcript. |
| `artifacts_partially_captured` | Capture wrote available artifacts, but transcript or audio was unavailable. | Inspect `manifest.json`; retry capture later if Retell is still processing. |
| `artifacts_capture_failed` | Capture could not retrieve usable Retell call details. | Inspect command error and provider status before retrying. |
| `analysis_completed` | Evidence-linked advisory analysis artifacts were written. | Human review of analysis and raw artifacts. |

## Required Artifacts

Completeness is status-aware:

| Run state | Required artifacts |
| --- | --- |
| Dry run | `scenario.yaml`, `metadata.json`, `patient_simulation.md`, `transcript.txt`, `observations.md` |
| Retell call start | `scenario.yaml`, `metadata.json`, `patient_simulation.md`, `observations.md` |
| Captured Retell run | call-start artifacts plus `transcript.json`, `transcript.txt`, `manifest.json` |
| Analyzed run | captured-run artifacts plus `analysis.json`, `analysis.md` |

`audio.wav` is optional because Retell may not provide a recording URL or the
download may fail. The manifest and run inspection warning explain the reason.

## Reproducibility Metadata

Phase 12 adds a `reproducibility` object to newly written `metadata.json` files:

```json
{
  "reproducibility": {
    "command": "capture-artifacts",
    "app_version": "0.0.1-SNAPSHOT",
    "git_commit": "optional"
  }
}
```

The field records the command that produced the current metadata state. Older
metadata without this object remains valid.

## Troubleshooting

If `--show-run` reports missing required artifacts, inspect the listed paths
first. A missing path usually means the run has not reached that lifecycle step.

If capture is partial, read `manifest.json`. Audio gaps are often expected;
missing transcript turns usually mean Retell has not finished processing or the
call produced no transcript content.

If conversation review reports duration as `unknown`, capture may have happened
before Retell finalized the call, or the provider may not have returned duration
or timestamp data. Retry capture after the call finishes before treating the run
as shallow.

If conversation review reports a `short` duration, use the cited transcript
turns to decide whether the call ended before the patient goal, workflow-specific
question, or confirmation/next-step evidence appeared. A short duration is an
advisory review signal, not an automated failure.

If analysis fails, confirm `transcript.json` exists and contains at least one
turn. The analyzer will reject unsupported evidence instead of writing a report.

If the run index looks noisy, filter it by scenario, status, or run mode. The
index is append-only, so a single `call_id` can appear multiple times as the run
gains artifacts.
