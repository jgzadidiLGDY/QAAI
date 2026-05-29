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
		@JsonProperty("multi_lens_review_json")
		String multiLensReviewJson,
		@JsonProperty("multi_lens_review_markdown")
		String multiLensReviewMarkdown,
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
				analysisMarkdown, null, null, null, null, observationsMarkdown);
	}

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
			String evaluationJson,
			String evaluationMarkdown,
			String observationsMarkdown
	) {
		this(scenario, metadata, transcriptText, transcriptJson, patientSimulation, audio, manifest, analysisJson,
				analysisMarkdown, evaluationJson, evaluationMarkdown, null, null, observationsMarkdown);
	}
}
