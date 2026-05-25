package com.qaai.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AnalysisFinding(
		String title,
		String severity,
		String workflow,
		@JsonProperty("expected_behavior")
		String expectedBehavior,
		@JsonProperty("actual_behavior")
		String actualBehavior,
		List<EvidenceReference> evidence
) {
}
