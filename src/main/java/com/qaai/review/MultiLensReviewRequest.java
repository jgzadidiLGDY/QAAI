package com.qaai.review;

import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.scenario.Scenario;
import java.nio.file.Path;
import java.time.OffsetDateTime;

public record MultiLensReviewRequest(
		String reviewId,
		OffsetDateTime generatedAt,
		Scenario scenario,
		RunMetadata metadata,
		NormalizedTranscript transcript,
		Path analysisJson,
		Path evaluationJson
) {
}
