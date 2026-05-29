package com.qaai.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ReviewLensResult(
		@JsonProperty("lens_id")
		String lensId,
		@JsonProperty("lens_label")
		String lensLabel,
		String status,
		String summary,
		List<ReviewFinding> findings,
		List<String> warnings
) {
}
