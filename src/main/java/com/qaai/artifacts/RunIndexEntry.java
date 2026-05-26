package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record RunIndexEntry(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		@JsonProperty("run_mode")
		String runMode,
		String status,
		@JsonProperty("retell_call_id")
		String retellCallId,
		@JsonProperty("started_at")
		OffsetDateTime startedAt,
		@JsonProperty("ended_at")
		OffsetDateTime endedAt,
		@JsonProperty("run_directory")
		String runDirectory,
		@JsonProperty("metadata_path")
		String metadataPath,
		@JsonProperty("complete")
		boolean complete,
		@JsonProperty("missing_required_artifacts")
		List<String> missingRequiredArtifacts,
		List<String> warnings,
		@JsonProperty("artifact_paths")
		ArtifactPaths artifactPaths
) {
}
