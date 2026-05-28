package com.qaai.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ReportRunSummary(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		@JsonProperty("run_mode")
		String runMode,
		String status,
		boolean complete,
		List<String> warnings,
		@JsonProperty("metadata_path")
		String metadataPath,
		@JsonProperty("transcript_path")
		String transcriptPath,
		@JsonProperty("analysis_path")
		String analysisPath,
		@JsonProperty("evaluation_path")
		String evaluationPath
) {
}
