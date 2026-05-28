package com.qaai.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record EvaluationDimensionResult(
		String name,
		Integer score,
		String scale,
		String rationale,
		String uncertainty,
		@JsonProperty("insufficient_evidence")
		boolean insufficientEvidence,
		List<EvaluationEvidenceReference> evidence
) {
}
