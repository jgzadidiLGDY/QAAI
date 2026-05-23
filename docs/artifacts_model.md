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

Human notes from reviewing a run, especially before/after conversation quality observations.

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
