package com.qaai.runner;

import com.qaai.artifacts.ArtifactBundle;
import com.qaai.artifacts.RunMetadata;

public record ScenarioRunResult(
		RunMetadata metadata,
		ArtifactBundle artifacts
) {
}
