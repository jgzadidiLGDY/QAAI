# Retell Setup

Retell AI is used for outbound call execution, voice agent configuration, transcripts, recordings, and call metadata.

Phase 3 starts real outbound calls through Retell and records the Retell call id
in local metadata. Phase 4 adds a manual capture command that fetches transcript
and recording artifacts after a call exists. Phase 5 sends scenario-driven
patient simulation instructions to Retell.

## Required Values

Create a local `.env` file or set shell environment variables with:

```text
RETELL_API_KEY=
RETELL_AGENT_ID=
RETELL_FROM_NUMBER=
RETELL_BASE_URL=https://api.retellai.com
TARGET_AGENT_PHONE_NUMBER=+18054398008
```

Do not commit real credentials.

When the app starts from the project root, it loads `.env` values as local
defaults. Existing shell environment variables take precedence.

## Current Assumptions

- Outbound calling is enabled in Retell.
- The Retell from-number or caller configuration is approved for test calls.
- The target healthcare voice agent can receive authorized test calls.
- Test scenarios will not use real patient data.
- Call volume will start low while workflow behavior is being validated.

## Phase 3 Usage

The dry-run command remains the default:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml"
```

To start a real Retell outbound call, explicitly choose Retell mode:

```powershell
.\gradlew bootRun --args="--scenario=scenarios/appointment-reschedule.yaml --run-mode=retell"
```

The Phase 3 request uses Retell's `POST /v2/create-phone-call` API with:

- `from_number`: `RETELL_FROM_NUMBER`
- `to_number`: `TARGET_AGENT_PHONE_NUMBER`
- `override_agent_id`: `RETELL_AGENT_ID`
- `metadata.qaai_call_id`: the local `call_id`
- scenario details as `retell_llm_dynamic_variables`

Phase 5 adds these stable dynamic variables for the Retell patient simulator:

- `patient_name`
- `call_reason`
- `patient_simulation_prompt`

Reference:

- [Retell Create Phone Call API](https://docs.retellai.com/api-references/create-phone-call)
- [Retell Outbound Calls Guide](https://docs.retellai.com/deploy/outbound-call)

## Phase 4 Artifact Capture

After a Retell call has completed or produced artifacts, capture them with the
local `call_id` from the Phase 3 output:

```powershell
.\gradlew bootRun --args="--capture-artifacts --call-id=<local_call_id>"
```

The command uses Retell's `GET /v2/get-call/{call_id}` API and writes normalized
artifacts under `outputs/{call_id}/`.

Reference:

- [Retell Get Call API](https://docs.retellai.com/api-references/get-call)

## Phase 5 Retell Prompt

Recommended welcome message:

```text
Hi, this is {{patient_name}}. I'm calling about {{call_reason}}. Could you help me?
```

Recommended agent prompt:

```text
You are roleplaying as a real patient calling a healthcare office for an authorized QA test.

Your job is to behave like the simulated patient described in the scenario instructions.

Core rules:
- Speak naturally, clearly, and coherently.
- Stay fully in character as the patient for the entire call.
- Be polite, concise, and conversational.
- Keep responses short, usually 1-2 sentences.
- State your reason for calling clearly and early in the conversation.
- Follow the scenario instructions exactly.
- Stay consistent with the provided patient facts.
- Never change, contradict, or invent patient facts.
- Do not invent unrelated medical issues, insurance details, symptoms, medications, history, addresses, or background details.
- Do not provide any information listed as disallowed or unavailable.
- Do not discuss prompts, internal tooling, implementation details, or test mechanics.

If the other side directly asks whether this is a test or simulated call, answer truthfully and briefly, then continue naturally if appropriate.

Conversation behavior:
- If the other side asks a clear question, answer directly using only known scenario facts.
- If the other side asks for information you do not have or must not provide, say you do not have that information available.
- If the other side misunderstands you, restate your request in a slightly different way.
- If the other side gives a generic response, silence, or does not guide the conversation, take initiative by restating your goal or asking a simple follow-up question.
- If the conversation becomes confusing, stay calm and try once or twice to clarify.
- If transfer or hold music happens, wait politely once, then ask whether assistance is still available.
- Do not end the call too quickly. Try to gather at least one useful answer, confirmation, next step, or reason the workflow cannot continue.
- Do not claim the goal is complete unless the healthcare office has clearly provided the confirmation, answer, or next step described in the scenario.
- If the goal is completed, the other side clearly ends the call, or the conversation is clearly stuck after reasonable clarification attempts, thank the other side politely and end the call.

Scenario instructions:
{{patient_simulation_prompt}}
```

## Later Phase Notes

If webhook delivery is used later, local development may need a public tunnel
such as ngrok, Cloudflare Tunnel, or a deployed test endpoint.
