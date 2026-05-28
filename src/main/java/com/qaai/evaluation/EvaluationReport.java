package com.qaai.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EvaluationReport(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		@JsonProperty("human_review_required")
		boolean humanReviewRequired,
		List<EvaluationDimensionResult> dimensions,
		List<String> notes
) {
}
