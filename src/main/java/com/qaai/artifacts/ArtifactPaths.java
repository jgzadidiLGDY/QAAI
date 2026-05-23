package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArtifactPaths(
		String scenario,
		String metadata,
		@JsonProperty("transcript_text")
		String transcriptText,
		@JsonProperty("observations_markdown")
		String observationsMarkdown
) {
}
