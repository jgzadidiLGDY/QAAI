package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record RunMetadata(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		@JsonProperty("run_mode")
		String runMode,
		@JsonProperty("target_phone_number")
		String targetPhoneNumber,
		@JsonProperty("retell_call_id")
		String retellCallId,
		@JsonProperty("started_at")
		OffsetDateTime startedAt,
		@JsonProperty("ended_at")
		OffsetDateTime endedAt,
		String status,
		@JsonProperty("artifact_paths")
		ArtifactPaths artifactPaths,
		AnalysisMetadata analysis,
		ReproducibilityMetadata reproducibility
) {

	public RunMetadata(
			String callId,
			String scenarioId,
			String runMode,
			String targetPhoneNumber,
			String retellCallId,
			OffsetDateTime startedAt,
			OffsetDateTime endedAt,
			String status,
			ArtifactPaths artifactPaths
	) {
		this(callId, scenarioId, runMode, targetPhoneNumber, retellCallId, startedAt, endedAt, status, artifactPaths,
				null, null);
	}

	public RunMetadata(
			String callId,
			String scenarioId,
			String runMode,
			String targetPhoneNumber,
			String retellCallId,
			OffsetDateTime startedAt,
			OffsetDateTime endedAt,
			String status,
			ArtifactPaths artifactPaths,
			AnalysisMetadata analysis
	) {
		this(callId, scenarioId, runMode, targetPhoneNumber, retellCallId, startedAt, endedAt, status, artifactPaths,
				analysis, null);
	}
}
