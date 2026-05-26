package com.qaai.quality;

import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;

public record ConversationQualityReviewResult(
		RunMetadata metadata,
		Path runDirectory,
		Path observationsMarkdown
) {
}
