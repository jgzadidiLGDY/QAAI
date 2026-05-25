package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArtifactPaths(
		String scenario,
		String metadata,
		@JsonProperty("transcript_text")
		String transcriptText,
		@JsonProperty("transcript_json")
		String transcriptJson,
		String audio,
		String manifest,
		@JsonProperty("observations_markdown")
		String observationsMarkdown
) {
}
