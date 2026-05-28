package com.qaai.scenariogeneration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qaai.scenario.Scenario;
import java.util.List;

public record ScenarioGenerationDraftSet(
		@JsonProperty("coverage_plan_markdown")
		String coveragePlanMarkdown,
		List<Scenario> scenarios
) {
}
