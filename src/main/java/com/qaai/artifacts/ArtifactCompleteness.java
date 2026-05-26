package com.qaai.artifacts;

import java.util.List;

public record ArtifactCompleteness(
		boolean complete,
		List<ArtifactStatus> artifacts,
		List<String> missingRequiredArtifacts,
		List<String> warnings
) {

	public record ArtifactStatus(
			String name,
			String path,
			boolean required,
			boolean present,
			String note
	) {
	}
}
