package com.qaai.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ReportModel(
		@JsonProperty("report_id")
		String reportId,
		@JsonProperty("generated_at")
		OffsetDateTime generatedAt,
		@JsonProperty("human_review_required")
		boolean humanReviewRequired,
		@JsonProperty("runs")
		List<ReportRunSummary> runs,
		@JsonProperty("evaluation_scores")
		Map<String, ReportEvaluationSummary> evaluationScores,
		@JsonProperty("severity_counts")
		Map<String, Long> severityCounts,
		@JsonProperty("coverage")
		List<ReportScenarioCoverageSummary> coverage
) {
}
