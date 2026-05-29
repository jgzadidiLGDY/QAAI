package com.qaai.review;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewEvidenceReference(
		String source,
		String speaker,
		String quote,
		@JsonProperty("turn_index")
		Integer turnIndex
) {
}
