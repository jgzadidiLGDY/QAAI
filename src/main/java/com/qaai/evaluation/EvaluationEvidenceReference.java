package com.qaai.evaluation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EvaluationEvidenceReference(
		String artifact,
		String speaker,
		String quote,
		@JsonProperty("turn_index")
		Integer turnIndex
) {
}
