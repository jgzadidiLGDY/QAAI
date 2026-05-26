# Project Specs

This document describes what the Voice AI QA Agent is expected to become, what the first small version should do, and what remains intentionally out of scope.

## Product Goal

Build a reproducible QA platform for testing healthcare voice agents through real outbound calls and scenario-driven patient behavior.

The platform should help a human reviewer answer:

- Was the target voice agent reachable?
- Did the patient simulation follow the scenario?
- Did the conversation complete the expected workflow?
- What transcript and recording evidence supports any finding?
- Can the same scenario be rerun and compared later?

## Current Target

The initial target healthcare voice agent is:

```text
+18054398008
```

All calls must use authorized test scenarios and avoid real patient data.

## Small Project Scope

The first useful version should:

- load a patient scenario
- execute a deterministic dry run
- place an outbound Retell call in a later phase
- capture transcript, audio, and metadata artifacts
- normalize artifacts under `outputs/{call_id}/`
- run AI-assisted analysis over completed artifacts
- produce structured bug findings with transcript evidence

## MVP+ Scope

The MVP+ project stage starts after the basic end-to-end workflow exists:
scenario execution, Retell call start, artifact capture, AI-assisted analysis,
run indexing, and conversation-quality review.

MVP+ should make that workflow reliable enough for repeated local QA use by a
human reviewer. The system should:

- bound external Retell, OpenAI, and recording-download calls with timeouts
- report provider and command failures with useful local context
- log important lifecycle events without exposing secrets or full transcript content
- keep AI analysis behind a pluggable module boundary
- support a deterministic local analyzer for tests, demos, and offline review
- make runs easy to inspect through focused CLI commands
- record enough metadata to reproduce or compare a run later
- keep README and operator docs accurate for setup, run, capture, review, analyze, and test workflows

MVP+ should not add broad orchestration, autonomous pass/fail decisions, or a
database unless a later milestone proves they are necessary.

## Non-Goals

This project is not:

- a production healthcare voice platform
- a clinical decision system
- a source of truth for pass/fail decisions
- a replacement for human QA review
- a place to store real patient data

## Users

Primary users:

- QA reviewer
- voice agent builder
- product owner reviewing workflow reliability

Secondary users:

- developer extending the runner
- reviewer auditing artifacts and analysis

## Core Workflows

Initial scenario workflows:

- appointment scheduling
- appointment rescheduling
- prescription refill
- billing question
- insurance verification

## Success Criteria

A phase is successful when:

- behavior is runnable locally
- execution flow is understandable
- artifacts are inspectable
- docs are current
- tests cover the behavior introduced
- scope remains controlled

An MVP+ milestone is successful when:

- external calls have timeout and error behavior covered by tests
- artifacts remain linked by `call_id` and are inspectable on disk
- analysis remains advisory, evidence-linked, and replaceable
- run inspection helps a reviewer understand current state without reading every file manually
- setup, run, capture, review, analyze, and test commands are documented accurately

## AI Boundary

AI may assist with:

- transcript summarization
- workflow issue detection
- issue clustering
- report generation

AI must not:

- control the core workflow
- fabricate evidence
- own final pass/fail decisions
- replace human review
