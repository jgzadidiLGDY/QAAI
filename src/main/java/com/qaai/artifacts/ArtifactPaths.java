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
		@JsonProperty("evaluation_json")
		String evaluationJson,
		@JsonProperty("evaluation_markdown")
		String evaluationMarkdown,
		@JsonProperty("observations_markdown")
		String observationsMarkdown
) {
	public ArtifactPaths(
			String scenario,
			String metadata,
			String transcriptText,
			String transcriptJson,
			String patientSimulation,
			String audio,
			String manifest,
			String analysisJson,
			String analysisMarkdown,
			String observationsMarkdown
	) {
		this(scenario, metadata, transcriptText, transcriptJson, patientSimulation, audio, manifest, analysisJson,
				analysisMarkdown, null, null, observationsMarkdown);
	}
}
