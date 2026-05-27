package com.qaai.runner;

import com.qaai.artifacts.ArtifactCompleteness;
import com.qaai.artifacts.RunIndexEntry;
import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;

public record RunInspection(
		RunMetadata metadata,
		Path runDirectory,
		ArtifactCompleteness completeness,
		RunIndexEntry latestIndexEntry
) {
}
