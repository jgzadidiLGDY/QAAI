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

### `transcript.txt`

Human-readable transcript for quick inspection.

### `transcript.json`

Normalized transcript with speaker labels, timestamps when available, and source metadata.

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

Later phases may maintain:

```text
outputs/index.jsonl
```

Each line should summarize one run and point to its artifact bundle.

## Evidence Rules

Bug findings must be grounded in artifacts:

- transcript evidence must quote or reference transcript content
- recording evidence must reference the recording artifact when used
- metadata claims must come from `metadata.json` or Retell source data
- analysis must not invent facts absent from the artifacts

## Git Rules

Generated artifacts under `outputs/` are ignored by Git except for `outputs/.gitkeep`.

Scenario definitions, docs, and code should be committed. Real credentials, recordings, and generated run outputs should not be committed.
