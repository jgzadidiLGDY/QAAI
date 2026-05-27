# Scenario Coverage

Phase 14 expands scenarios through a curated coverage model. The goal is not a
large scenario dump. Each scenario should explain the workflow area and the
edge-case risk it is meant to exercise.

Coverage metadata is reviewer-facing. It should help operators choose a
scenario and interpret artifacts without turning scenario coverage into an
automatic pass/fail decision.

## Coverage Fields

Each scenario includes:

```yaml
coverage:
  workflow_area: appointment_rescheduling
  edge_cases:
    - missing_fact
  risk_focus: Confirm whether the office can continue when a required detail is unavailable.
```

- `workflow_area`: the workflow under review.
- `edge_cases`: one or more known coverage tags.
- `risk_focus`: a short explanation of what the scenario is meant to reveal.

Allowed edge-case tags:

- `happy_path`
- `missing_fact`
- `clarification`
- `transfer_or_hold`
- `ambiguous_next_step`
- `unavailable_information`
- `workflow_recovery`
- `workflow_mismatch`

## Scenario Map

| Scenario | Workflow area | Edge cases | Risk focus |
| --- | --- | --- | --- |
| `appointment-reschedule.yaml` | `appointment_rescheduling` | `happy_path` | Confirm a specific new appointment date and time without unsupported patient details. |
| `appointment-scheduling.yaml` | `appointment_scheduling` | `happy_path` | Verify that routine scheduling produces an appointment option or clear scheduling next step. |
| `billing-question.yaml` | `billing_question` | `workflow_recovery` | Check whether a general front desk response can still route the patient to billing support. |
| `insurance-verification.yaml` | `insurance_verification` | `unavailable_information` | Exercise the office response when the patient cannot provide insurance identifiers. |
| `prescription-refill.yaml` | `prescription_refill` | `missing_fact` | Confirm that refill guidance remains useful without medication details. |
| `appointment-reschedule-missing-details.yaml` | `appointment_rescheduling` | `missing_fact`, `clarification` | Exercise rescheduling when the current appointment date is unavailable. |
| `billing-transfer-hold.yaml` | `billing_question` | `transfer_or_hold`, `workflow_recovery` | Check recovery after transfer or hold behavior. |
| `insurance-verification-ambiguous-next-step.yaml` | `insurance_verification` | `unavailable_information`, `ambiguous_next_step` | Turn vague insurance verification guidance into a concrete next step. |
| `referral-status-workflow-mismatch.yaml` | `referral_status` | `workflow_mismatch`, `workflow_recovery` | Recover when the request does not fit the existing scheduling, billing, refill, or insurance flows. |

## Review Guidance

Use coverage metadata to select a scenario for a specific QA question:

- For prompt stability, start with a `happy_path` scenario.
- For hallucination pressure, use `missing_fact` or `unavailable_information`.
- For pacing and resilience, use `transfer_or_hold` or `workflow_recovery`.
- For target workflow boundaries, use `workflow_mismatch`.
- For completion clarity, use `ambiguous_next_step`.

The reviewer should still inspect transcript, metadata, observations, audio,
and analysis artifacts. Coverage tags explain intent; they do not decide
whether the target workflow passed.
