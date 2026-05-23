# Retell Setup

Retell AI will be used for outbound call execution, voice agent configuration, transcripts, recordings, and call metadata.

Phase 0 does not call Retell. It only defines the configuration contract that later phases will use.

## Required Values

Create a local `.env` file or set shell environment variables with:

```text
RETELL_API_KEY=
RETELL_AGENT_ID=
RETELL_FROM_NUMBER=
TARGET_AGENT_PHONE_NUMBER=+18054398008
```

Do not commit real credentials.

## Current Assumptions

- Outbound calling is enabled in Retell.
- The Retell from-number or caller configuration is approved for test calls.
- The target healthcare voice agent can receive authorized test calls.
- Test scenarios will not use real patient data.
- Call volume will start low while workflow behavior is being validated.

## Later Phase Notes

Phase 2 will add the Retell outbound call client.

Phase 3 will add transcript, recording, and call metadata capture. If webhook delivery is used, local development may need a public tunnel such as ngrok, Cloudflare Tunnel, or a deployed test endpoint.
