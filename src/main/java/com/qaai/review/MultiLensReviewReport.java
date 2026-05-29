package com.qaai.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record MultiLensReviewReport(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		@JsonProperty("review_id")
		String reviewId,
		@JsonProperty("generated_at")
		OffsetDateTime generatedAt,
		String provider,
		String model,
		@JsonProperty("human_review_required")
		boolean humanReviewRequired,
		List<ReviewLensResult> lenses,
		List<String> notes
) {
}
