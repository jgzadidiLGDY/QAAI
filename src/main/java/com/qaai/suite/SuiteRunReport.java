package com.qaai.suite;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record SuiteRunReport(
		@JsonProperty("suite_run_id")
		String suiteRunId,
		@JsonProperty("suite_id")
		String suiteId,
		@JsonProperty("agent_profile_id")
		String agentProfileId,
		@JsonProperty("generated_at")
		OffsetDateTime generatedAt,
		@JsonProperty("run_mode")
		String runMode,
		@JsonProperty("human_review_required")
		boolean humanReviewRequired,
		List<SuiteScenarioRunSummary> runs,
		List<String> warnings
) {
	public record SuiteScenarioRunSummary(
			@JsonProperty("scenario_path")
			String scenarioPath,
			@JsonProperty("scenario_id")
			String scenarioId,
			@JsonProperty("call_id")
			String callId,
			String status,
			boolean complete,
			@JsonProperty("metadata_path")
			String metadataPath,
			@JsonProperty("transcript_text_path")
			String transcriptTextPath,
			@JsonProperty("transcript_json_path")
			String transcriptJsonPath,
			List<String> warnings
	) {
	}
}
