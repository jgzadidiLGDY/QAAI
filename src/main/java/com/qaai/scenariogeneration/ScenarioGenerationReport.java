package com.qaai.scenariogeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ScenarioGenerationReport(
		@JsonProperty("generation_id")
		String generationId,
		@JsonProperty("generated_at")
		OffsetDateTime generatedAt,
		@JsonProperty("agent_description")
		String agentDescription,
		String provider,
		String model,
		@JsonProperty("human_review_required")
		boolean humanReviewRequired,
		@JsonProperty("draft_paths")
		List<String> draftPaths,
		@JsonProperty("validation_results")
		List<ScenarioDraftValidationResult> validationResults,
		@JsonProperty("coverage_by_workflow")
		Map<String, Long> coverageByWorkflow,
		@JsonProperty("coverage_by_edge_case")
		Map<String, Long> coverageByEdgeCase,
		List<String> warnings
) {
}
