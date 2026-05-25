package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArtifactPaths(
		String scenario,
		String metadata,
		@JsonProperty("transcript_text")
		String transcriptText,
		@JsonProperty("transcript_json")
		String transcriptJson,
		@JsonProperty("patient_simulation")
		String patientSimulation,
		String audio,
		String manifest,
		@JsonProperty("analysis_json")
		String analysisJson,
		@JsonProperty("analysis_markdown")
		String analysisMarkdown,
		@JsonProperty("observations_markdown")
		String observationsMarkdown
) {
}
