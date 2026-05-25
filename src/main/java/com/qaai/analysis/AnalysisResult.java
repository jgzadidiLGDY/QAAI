package com.qaai.analysis;

import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;

public record AnalysisResult(
		RunMetadata metadata,
		Path runDirectory,
		Path analysisJson,
		Path analysisMarkdown
) {
}
