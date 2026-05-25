package com.qaai.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AnalysisReport(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("scenario_id")
		String scenarioId,
		String summary,
		@JsonProperty("human_review_required")
		boolean humanReviewRequired,
		List<AnalysisFinding> findings,
		List<String> notes
) {
}
