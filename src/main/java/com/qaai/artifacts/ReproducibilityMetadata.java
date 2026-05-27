package com.qaai.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReproducibilityMetadata(
		String command,
		@JsonProperty("app_version")
		String appVersion,
		@JsonProperty("git_commit")
		String gitCommit
) {
}
