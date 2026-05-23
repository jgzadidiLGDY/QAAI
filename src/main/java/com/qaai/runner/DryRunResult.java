package com.qaai.runner;

import com.qaai.artifacts.ArtifactBundle;
import com.qaai.artifacts.RunMetadata;

public record DryRunResult(
		RunMetadata metadata,
		ArtifactBundle artifacts
) {
}
