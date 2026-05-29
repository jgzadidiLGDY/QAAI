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
		String channel,
		@JsonProperty("target_phone_number")
		String targetPhoneNumber,
		@JsonProperty("retell_call_id")
		String retellCallId,
		@JsonProperty("started_at")
		OffsetDateTime startedAt,
		@JsonProperty("ended_at")
		OffsetDateTime endedAt,
		@JsonProperty("call_duration_seconds")
		Long callDurationSeconds,
		String status,
		@JsonProperty("artifact_paths")
		ArtifactPaths artifactPaths,
		AnalysisMetadata analysis,
		EvaluationMetadata evaluation,
		ReproducibilityMetadata reproducibility
) {
	public RunMetadata {
		channel = normalizeChannel(channel, runMode);
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
			ArtifactPaths artifactPaths
	) {
		this(callId, scenarioId, runMode, defaultChannel(runMode), targetPhoneNumber, retellCallId, startedAt, endedAt,
				null, status, artifactPaths, null, null, null);
	}

	public RunMetadata(
			String callId,
			String scenarioId,
			String runMode,
			String channel,
			String targetPhoneNumber,
			String retellCallId,
			OffsetDateTime startedAt,
			OffsetDateTime endedAt,
			String status,
			ArtifactPaths artifactPaths
	) {
		this(callId, scenarioId, runMode, channel, targetPhoneNumber, retellCallId, startedAt, endedAt, null,
				status, artifactPaths, null, null, null);
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
		this(callId, scenarioId, runMode, defaultChannel(runMode), targetPhoneNumber, retellCallId, startedAt, endedAt,
				null, status, artifactPaths, analysis, null, null);
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
			AnalysisMetadata analysis,
			ReproducibilityMetadata reproducibility
	) {
		this(callId, scenarioId, runMode, defaultChannel(runMode), targetPhoneNumber, retellCallId, startedAt, endedAt,
				null, status, artifactPaths, analysis, null, reproducibility);
	}

	public RunMetadata(
			String callId,
			String scenarioId,
			String runMode,
			String targetPhoneNumber,
			String retellCallId,
			OffsetDateTime startedAt,
			OffsetDateTime endedAt,
			Long callDurationSeconds,
			String status,
			ArtifactPaths artifactPaths,
			AnalysisMetadata analysis,
			ReproducibilityMetadata reproducibility
	) {
		this(callId, scenarioId, runMode, defaultChannel(runMode), targetPhoneNumber, retellCallId, startedAt, endedAt,
				callDurationSeconds, status, artifactPaths, analysis, null, reproducibility);
	}

	public RunMetadata(
			String callId,
			String scenarioId,
			String runMode,
			String targetPhoneNumber,
			String retellCallId,
			OffsetDateTime startedAt,
			OffsetDateTime endedAt,
			Long callDurationSeconds,
			String status,
			ArtifactPaths artifactPaths,
			AnalysisMetadata analysis,
			EvaluationMetadata evaluation,
			ReproducibilityMetadata reproducibility
	) {
		this(callId, scenarioId, runMode, defaultChannel(runMode), targetPhoneNumber, retellCallId, startedAt, endedAt,
				callDurationSeconds, status, artifactPaths, analysis, evaluation, reproducibility);
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
			AnalysisMetadata analysis,
			EvaluationMetadata evaluation,
			ReproducibilityMetadata reproducibility
	) {
		this(callId, scenarioId, runMode, defaultChannel(runMode), targetPhoneNumber, retellCallId, startedAt, endedAt,
				null, status,
				artifactPaths, analysis, evaluation, reproducibility);
	}

	private static String normalizeChannel(String channel, String runMode) {
		if (channel != null && !channel.isBlank()) {
			return channel;
		}
		return defaultChannel(runMode);
	}

	private static String defaultChannel(String runMode) {
		if ("retell".equals(runMode) || "dry_run".equals(runMode)) {
			return "voice";
		}
		return "unknown";
	}
}
