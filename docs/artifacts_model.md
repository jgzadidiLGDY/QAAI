# Artifacts Model

This document defines the planned artifact conventions for QA runs.

## Core Rule

Every run is linked by a generated `call_id`.

All run artifacts should live under:

```text
outputs/{call_id}/
```

The `call_id` is the local project identifier. External identifiers, such as Retell call ids, should be recorded in metadata rather than replacing `call_id`.

## Planned Artifact Bundle

```text
outputs/{call_id}/
|-- scenario.yaml
|-- metadata.json
|-- transcript.txt
|-- transcript.json
|-- patient_simulation.md
|-- audio.wav
|-- analysis.json
|-- analysis.md
|-- manifest.json
`-- observations.md
```

Not every phase produces every artifact. Each phase should document which artifacts are expected.

## Phase 1 Artifact Bundle

Phase 1 dry runs produce:

```text
outputs/{call_id}/
|-- scenario.yaml
|-- metadata.json
`-- transcript.txt
```

The metadata must clearly mark the local-only execution mode:

```json
{
  "run_mode": "dry_run",
  "retell_call_id": null
}
```

## Phase 2 Artifact Bundle

Phase 2 dry runs add conversation-quality observations while preserving the
Phase 1 artifact model:

```text
outputs/{call_id}/
|-- scenario.yaml
|-- metadata.json
|-- transcript.txt
`-- observations.md
```

`metadata.json` includes an `artifact_paths.observations_markdown` entry.

## Phase 3 Artifact Bundle

Phase 3 Retell runs start a real outbound call and persist the local-to-Retell
identifier mapping:

```text
outputs/{call_id}/
|-- scenario.yaml
|-- metadata.json
`-- observations.md
```

The metadata must mark the real-call execution mode:

```json
{
  "run_mode": "retell",
  "retell_call_id": "retell-call-id-from-api",
  "status": "retell_registered"
}
```

`artifact_paths.transcript_text` is `null` in Phase 3 because transcript capture
belongs to Phase 4.

## Phase 4 Artifact Bundle

Phase 4 adds a manual artifact capture step for an existing Retell run:

```powershell
.\gradlew bootRun --args="--capture-artifacts --call-id=<local_call_id>"
```

The command reads `outputs/{call_id}/metadata.json`, uses the stored
`retell_call_id`, fetches call details from Retell, and writes:

```text
outputs/{call_id}/
|-- scenario.yaml
|-- metadata.json
|-- patient_simulation.md
|-- transcript.json
|-- transcript.txt
|-- audio.wav
|-- manifest.json
`-- observations.md
```

`audio.wav` is only present when Retell provides a recording URL and the
download succeeds. Missing transcript or audio data is recorded in
`manifest.json` and reflected by `metadata.status = artifacts_partially_captured`.

## Artifact Purposes

### `scenario.yaml`

Snapshot of the scenario used for the run. This preserves reproducibility even if the source scenario changes later.

### `metadata.json`

Machine-readable run metadata.

Expected fields over time:

- `call_id`
- `scenario_id`
- `run_mode`
- `target_phone_number`
- `retell_call_id`
- `started_at`
- `ended_at`
- `status`
- `artifact_paths`
- `analysis` after successful analysis, including analyzer provider and model

### `transcript.txt`

Human-readable transcript for quick inspection.

### `transcript.json`

Normalized transcript with speaker labels, timestamps when available, and source metadata.

For Retell calls, this project labels Retell `agent` transcript turns as
`patient` because the Retell AI agent is the simulated patient. Retell `user`
turns are labeled as `receptionist` because they represent the target
healthcare front desk side in this QA workflow.

### `patient_simulation.md`

Deterministic patient behavior instructions generated from the scenario. This is
the same scenario-specific prompt sent to Retell as `patient_simulation_prompt`.

### `audio.wav`

Downloaded or normalized call recording when available and permitted.

### `analysis.json`

Structured AI-assisted findings. Each finding must cite transcript evidence.

### `analysis.md`

Human-readable analysis report derived from `analysis.json`.

### `manifest.json`

Artifact inventory for the run. This should make completeness checks straightforward.

### `observations.md`

Before/after conversation-quality observations for a run. In Phase 2 this file
starts as a deterministic template grounded in the scenario guidance. Later
phases can add human notes after real call artifacts exist.

## Run Index

Phase 7 maintains:

```text
outputs/index.jsonl
```

Each line summarizes one completed artifact-write lifecycle step and points to
the run artifact bundle. A single `call_id` can appear more than once as the run
moves from call start to capture to analysis. The per-run `metadata.json`
remains the source of truth; the index is an append-only inspection aid.

Each index entry records:

- `call_id`
- `scenario_id`
- `run_mode`
- `status`
- `retell_call_id`
- start and end timestamps
- run directory and metadata path
- whether expected artifacts are complete for the current status
- missing required artifact names
- warnings, including unavailable optional audio
- the current metadata artifact paths

## Evidence Rules

Bug findings must be grounded in artifacts:

- transcript evidence must quote or reference transcript content
- recording evidence must reference the recording artifact when used
- metadata claims must come from `metadata.json` or Retell source data
- analysis must not invent facts absent from the artifacts

## Phase 5 Artifact Bundle

Phase 5 adds scenario-driven patient simulation instructions:

```text
outputs/{call_id}/
|-- scenario.yaml
|-- metadata.json
|-- patient_simulation.md
|-- transcript.txt
`-- observations.md
```

For Retell call-start runs, `transcript.txt` remains unavailable until artifact
capture, but `patient_simulation.md` is written at call start.

## Phase 6 Artifact Bundle

Phase 6 adds single-call AI-assisted analysis for captured calls:

```powershell
.\gradlew bootRun --args="--analyze-call --call-id=<local_call_id>"
```

The command requires an existing `outputs/{call_id}/transcript.json` artifact
and writes:

```text
outputs/{call_id}/analysis.json
outputs/{call_id}/analysis.md
```

`metadata.json` includes:

```json
{
  "status": "analysis_completed",
  "artifact_paths": {
    "analysis_json": "outputs/call_20260523_001/analysis.json",
    "analysis_markdown": "outputs/call_20260523_001/analysis.md"
  },
  "analysis": {
    "provider": "openai",
    "model": "gpt-4.1-mini"
  }
}
```

If `manifest.json` already exists, Phase 6 appends `analysis_json` and
`analysis_markdown` entries. AI-assisted findings are advisory and must be
grounded in exact transcript quotes.

## Phase 7 Artifact Bundle

Phase 7 adds an append-only run index and artifact completeness checks.

Any successful artifact write now appends an entry to:

```text
outputs/index.jsonl
```

The local inspection command is:

```powershell
.\gradlew bootRun --args="--list-runs"
```

Completeness is derived from the current `metadata.json` artifact paths and the
files present on disk. Required artifacts vary by lifecycle status:

- dry run: scenario, metadata, transcript text, patient simulation, observations
- Retell call start: scenario, metadata, patient simulation, observations
- artifact capture: scenario, metadata, transcript JSON/text, patient simulation, manifest, observations
- analysis: captured artifacts plus analysis JSON and Markdown

Audio remains optional because Retell may not provide a recording URL or a
downloadable recording at capture time.

## Git Rules

Generated artifacts under `outputs/` are ignored by Git except for `outputs/.gitkeep`.

Scenario definitions, docs, and code should be committed. Real credentials, recordings, and generated run outputs should not be committed.

## Phase 8 Artifact Bundle

Phase 8 adds deterministic conversation-quality review for an existing local run:

```powershell
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

The command refreshes:

```text
outputs/{call_id}/observations.md
```

It uses the scenario snapshot and, when available, normalized transcript turns to
cite evidence by turn number. Missing transcript evidence is recorded as
unavailable rather than inferred.

The command appends a run index entry after writing observations. It does not
change `metadata.status`, does not create pass/fail decisions, and does not
rewrite historical artifacts outside the requested `call_id`.

## Phase 10 Artifact Bundle

Phase 10 keeps the Phase 6 analysis artifact shape, but makes the analyzer
provider explicit. After successful analysis, `metadata.json` records:

```json
{
  "analysis": {
    "provider": "local",
    "model": "deterministic-v1"
  }
}
```

The supported providers are:

- `openai`: OpenAI-backed analysis, using `OPENAI_ANALYSIS_MODEL`
- `local`: deterministic local analyzer for offline workflow checks
- `disabled`: explicit mode that rejects analysis requests

All successful providers still write `analysis.json` and `analysis.md` through
the same service-level validation. Findings remain advisory, evidence-linked,
and human-reviewed.

## Phase 11 Inspection Commands

Phase 11 adds read-only local inspection commands. They do not write new run
artifacts and do not change `metadata.status`.

Inspect one run:

```powershell
.\gradlew bootRun --args="--show-run --call-id=<local_call_id>"
```

Filter the append-only run index:

```powershell
.\gradlew bootRun --args="--list-runs --scenario=appointment_reschedule_001"
.\gradlew bootRun --args="--list-runs --status=artifacts_partially_captured"
.\gradlew bootRun --args="--list-runs --run-mode=retell"
```

The one-run view reads `metadata.json`, checks artifact paths on disk, and uses
the run index only as an inspection aid. It reports completeness, missing
required artifacts, warnings, artifact paths, and suggested next local commands.

## MVP+ Artifact Trust Direction

Phases 9 through 12 should make artifact bundles easier to trust during repeated
real QA work.

Expected additions should remain local-first and additive:

- provider timeout and failure context should be visible in command output or logs
- metadata may record the command, analyzer provider, analyzer model, app version, and git commit when practical
- manifests should continue to describe missing or partial artifacts without fabricating completion
- run inspection commands should read existing artifacts rather than creating a competing source of truth
- docs should explain which artifacts are required, optional, unavailable, or advisory for each run state

The artifact model should continue avoiding committed run outputs, credentials,
recordings, and real patient data.
