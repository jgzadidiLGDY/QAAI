# Phase 14 Closeout

Phase 14 expanded scenario coverage through explicit, reviewer-facing coverage
metadata and a small curated set of synthetic edge-case scenarios.

## Completed

- Added `coverage` metadata to the scenario schema:
  - `workflow_area`
  - `edge_cases`
  - `risk_focus`
- Added deterministic validation for missing coverage fields and unsupported
  edge-case tags.
- Updated existing scenarios with coverage metadata.
- Changed scenario tests to discover and validate every `scenarios/*.yaml`
  file.
- Added curated edge-case scenarios for:
  - missing appointment details and clarification
  - billing transfer or hold recovery
  - ambiguous insurance verification next steps
  - referral status workflow mismatch
- Documented the coverage taxonomy and scenario-to-risk map in
  [Scenario Coverage](scenario-coverage.md).
- Preserved synthetic-only scenario data and human-owned review boundaries.

## Local Usage

Run any scenario with the existing deterministic dry-run path:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/billing-transfer-hold.yaml"
```

Start an authorized Retell call when live calibration is intended:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/billing-transfer-hold.yaml --run-mode=retell"
```

After capture:

```powershell
.\gradlew bootRun --args="--capture-artifacts --call-id=<local_call_id>"
.\gradlew bootRun --args="--review-conversation --call-id=<local_call_id>"
```

## Live Calibration

One authorized live Retell call was placed on May 27, 2026 for the new billing
transfer/hold scenario.

```text
call_id: call_20260527_150145_bf5524a6
retell_call_id: call_1b5cf396ee9e4273d7dfeabc7ce
scenario_id: billing_transfer_hold_001
call_duration_seconds: 42
turn_count: 18 total, 8 patient, 10 receptionist
duration_signal: short
```

Observed behavior:

- The patient stated the billing goal.
- The target side offered documentation/callback language and transfer/hold
  language.
- The call ended at the test line before a durable billing next step was
  confirmed.
- Conversation review correctly marked the duration as short, the
  workflow-specific receptionist question as not observed, and confirmation or
  next step as not observed.

This is useful calibration for the scenario: it exercises transfer/hold
behavior, but the captured call still needs human review before it can be used
as evidence of target workflow quality.

## Still Out Of Scope

- Automated pass/fail decisions.
- AI-generated scenario creation.
- Real patient data.
- Clinical quality judgment.
- Batch scenario execution.
- Provider-specific retry automation.

## Tests

```powershell
.\gradlew test --tests "*ScenarioLoaderTest" --tests "*ScenarioValidatorTest" --tests "*PatientSimulationPromptBuilderTest"
.\gradlew test --tests "*ScenarioLoaderTest" --tests "*ScenarioValidatorTest"
.\gradlew test
```

## Silver Notes

Behavior commits:

- Scenario coverage metadata:
  - Base commit: `215a7f6`
  - Fix commit: `7ed0ce9`
- Curated edge-case scenarios:
  - Base commit: `39c6d0a`
  - Fix commit: `4a704d9`

The strongest Phase 14 candidate is scenario coverage metadata because it
introduces deterministic validation behavior. The curated scenario commit is
more fixture-oriented, but dynamic scenario discovery still gives it a
deterministic validation path.
