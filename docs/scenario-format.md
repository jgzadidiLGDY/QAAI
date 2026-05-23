# Scenario Format

Scenarios define patient behavior and expected workflow outcomes. They are intended to make call execution reproducible and reviewable.

Phase 0 does not parse scenario files yet. Phase 1 will introduce the first scenario schema and deterministic dry-run behavior.

## Planned YAML Shape

```yaml
id: appointment_reschedule_001
name: Appointment reschedule
workflow: appointment_rescheduling

persona:
  name: Maria Lopez
  date_of_birth: 1982-04-19
  phone_number: "+15555550123"

goal:
  summary: Reschedule an existing appointment to next week.
  expected_outcome: Agent confirms a new appointment date and time.

constraints:
  allowed_facts:
    - Patient has an existing appointment.
    - Patient can attend next Tuesday or Wednesday morning.
  disallowed_behavior:
    - Do not invent insurance details.
    - Do not provide real patient data.

steps:
  - intent: greeting
    patient_says: Hi, I need to reschedule my appointment.
  - intent: identity_verification
    patient_says: My date of birth is April 19th, 1982.
```

## Artifact Expectations

Each scenario run should eventually produce artifacts under:

```text
outputs/{call_id}/
```

Phase 1 will define the first dry-run artifacts:

```text
outputs/{call_id}/scenario.yaml
outputs/{call_id}/metadata.json
outputs/{call_id}/transcript.txt
```
