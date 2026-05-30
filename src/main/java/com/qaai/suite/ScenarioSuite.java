package com.qaai.suite;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScenarioSuite(
		String id,
		@JsonProperty("agent_profile")
		String agentProfile,
		@JsonProperty("default_run_mode")
		String defaultRunMode,
		List<String> scenarios
) {
}
