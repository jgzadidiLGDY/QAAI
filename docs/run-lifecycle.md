# Run Lifecycle

The local workflow is intentionally explicit. Each command writes or inspects
artifacts under `outputs/{call_id}/`, and humans decide final QA outcomes.

## Command Order

```text
dry run:
  --scenario=<path>

text chat:
  --scenario=<path> --run-mode=text-chat

real call:
  --scenario=<path> --run-mode=retell
  --capture-artifacts --call-id=<local_call_id>
  --review-conversation --call-id=<local_call_id>
  --analyze-call --call-id=<local_call_id>
  --evaluate-call --call-id=<local_call_id>
  --multi-lens-review --call-id=<local_call_id>
```

`--evaluate-call` and `--multi-lens-review` can also run against a completed
text chat run because they use the normalized `transcript.json` artifact. Run
analysis, evaluation, and multi-lens review in whatever order is useful for the
review, but rerun downstream review artifacts after recapturing a Retell
transcript if the evidence changed.

`--show-run`, `--list-runs`, and `--generate-report` can be used at any point
after artifacts exist.

## Statuses

| Status | Meaning | Typical next step |
| --- | --- | --- |
| `completed` | Dry-run artifacts were written. | Inspect artifacts or start a Retell run. |
| `completed` with `channel = text` | Local text chat artifacts were written. | Inspect artifacts, review conversation, analyze, evaluate, or report. |
| `retell_*` | Retell accepted the outbound call request and returned a call id. | Capture artifacts after the call has produced details. |
| `started` | Retell accepted the call but returned no specific call status. | Capture artifacts after the call has produced details. |
| `artifacts_captured` | Transcript artifacts and audio were captured. | Review conversation or analyze transcript. |
| `artifacts_partially_captured` | Capture wrote available artifacts, but transcript or audio was unavailable. | Inspect `manifest.json`; retry capture later if Retell is still processing. |
| `artifacts_capture_failed` | Capture could not retrieve usable Retell call details. | Inspect command error and provider status before retrying. |
| `analysis_completed` | Evidence-linked advisory analysis artifacts were written. | Evaluate, run multi-lens review, generate a report, or inspect raw artifacts. |
| `evaluation_completed` | Rubric-specific advisory evaluation artifacts were written. | Analyze, run multi-lens review, generate a report, or inspect raw artifacts. |
| `multi_lens_review_completed` | Structured advisory multi-lens review artifacts were written. | Generate a report or inspect raw artifacts. |

## Required Artifacts

Completeness is status-aware:

| Run state | Required artifacts |
| --- | --- |
| Dry run | `scenario.yaml`, `metadata.json`, `patient_simulation.md`, `transcript.txt`, `observations.md` |
| Text chat run | `scenario.yaml`, `metadata.json`, `patient_simulation.md`, `transcript.txt`, `transcript.json`, `observations.md` |
| Retell call start | `scenario.yaml`, `metadata.json`, `patient_simulation.md`, `observations.md` |
| Captured Retell run | call-start artifacts plus `transcript.json`, `transcript.txt`, `manifest.json` |
| Analyzed run | captured-run artifacts plus `analysis.json`, `analysis.md` |
| Evaluated run | transcript artifacts plus `evaluation.json`, `evaluation.md`; voice runs also require `manifest.json` |
| Multi-lens reviewed run | transcript artifacts plus `multi-lens-review.json`, `multi-lens-review.md`; voice runs also require `manifest.json` |

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

For text chat runs, missing `audio.wav` and `manifest.json` are expected in the
local prototype. The required evidence is the normalized `transcript.json` plus
the human-readable `transcript.txt`.

If analysis fails, confirm `transcript.json` exists and contains at least one
turn. The analyzer will reject unsupported evidence instead of writing a report.

If evaluation or multi-lens review fails, confirm `transcript.json` exists and
that any cited evidence quotes are present in the normalized transcript. Missing
or weak evidence should be represented as insufficient evidence rather than
guessed.

If capture is rerun after analysis, evaluation, or multi-lens review already
exists, metadata preserves those artifact links for auditability. Treat them as
review history tied to the earlier transcript and rerun downstream commands when
the recaptured transcript materially changes.

If the run index looks noisy, filter it by scenario, status, or run mode. The
index is append-only, so a single `call_id` can appear multiple times as the run
gains artifacts.
