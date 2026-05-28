package com.qaai.scenariogeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScenarioDraftValidationResult(
		@JsonProperty("draft_path")
		String draftPath,
		@JsonProperty("scenario_id")
		String scenarioId,
		boolean valid,
		List<String> errors
) {
}
