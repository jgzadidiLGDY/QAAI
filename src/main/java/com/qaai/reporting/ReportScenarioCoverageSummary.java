package com.qaai.reporting;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ReportScenarioCoverageSummary(
		@JsonProperty("scenario_id")
		String scenarioId,
		@JsonProperty("workflow_area")
		String workflowArea,
		@JsonProperty("edge_cases")
		List<String> edgeCases,
		@JsonProperty("risk_focus")
		String riskFocus
) {
}
