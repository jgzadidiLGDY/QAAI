package com.qaai.review;

import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;

public record MultiLensReviewResult(
		RunMetadata metadata,
		Path runDirectory,
		Path reviewJson,
		Path reviewMarkdown
) {
}
