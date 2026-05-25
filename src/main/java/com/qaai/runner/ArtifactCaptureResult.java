package com.qaai.runner;

import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;

public record ArtifactCaptureResult(
		RunMetadata metadata,
		Path runDirectory,
		Path transcriptJson,
		Path transcriptText,
		Path audio,
		Path manifest
) {
}
