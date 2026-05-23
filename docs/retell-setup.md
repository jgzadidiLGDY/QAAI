# Retell Setup

Retell AI is used for outbound call execution, voice agent configuration, transcripts, recordings, and call metadata.

Phase 3 starts real outbound calls through Retell and records the Retell call id
in local metadata. It does not yet fetch transcripts, recordings, or final call
status.

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

Reference:

- [Retell Create Phone Call API](https://docs.retellai.com/api-references/create-phone-call)
- [Retell Outbound Calls Guide](https://docs.retellai.com/deploy/outbound-call)

## Later Phase Notes

Phase 4 should add transcript, recording, and call metadata capture. If webhook
delivery is used, local development may need a public tunnel such as ngrok,
Cloudflare Tunnel, or a deployed test endpoint.
