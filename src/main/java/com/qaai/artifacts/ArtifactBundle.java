package com.qaai.artifacts;

import java.nio.file.Path;

public record ArtifactBundle(
		String callId,
		Path runDirectory,
		Path scenarioSnapshot,
		Path metadata,
		Path transcriptText
) {
}
