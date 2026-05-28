package com.qaai.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReportEvaluationSummary(
		String dimension,
		@JsonProperty("scored_count")
		int scoredCount,
		@JsonProperty("average_score")
		Double averageScore,
		@JsonProperty("insufficient_evidence_count")
		int insufficientEvidenceCount
) {
}
