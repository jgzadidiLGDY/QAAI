package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record ArtifactManifest(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		@JsonProperty("retell_call_id")
		String retellCallId,
		@JsonProperty("captured_at")
		OffsetDateTime capturedAt,
		@JsonProperty("retell_status")
		String retellStatus,
		@JsonProperty("recording_url")
		String recordingUrl,
		List<ArtifactEntry> artifacts
) {

	public record ArtifactEntry(
			String name,
			String path,
			boolean present,
			String note
	) {
	}
}
