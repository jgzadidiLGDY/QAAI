package com.qaai.evaluation;

import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;

public record EvaluationResult(
		RunMetadata metadata,
		Path runDirectory,
		Path evaluationJson,
		Path evaluationMarkdown
) {
}
