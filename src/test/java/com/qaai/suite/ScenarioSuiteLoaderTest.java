package com.qaai.suite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qaai.agent.AgentProfileLoader;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioSuiteLoaderTest {

	@Test
	void loadsValidSuite() {
		ScenarioSuite suite = new ScenarioSuiteLoader().load(java.nio.file.Path.of("suites/receptionist-smoke.yaml"));

		assertThat(suite.id()).isEqualTo("receptionist_smoke_suite");
		assertThat(suite.agentProfile()).isEqualTo("medical_receptionist_demo");
		assertThat(suite.defaultRunMode()).isEqualTo("text-chat");
		assertThat(suite.scenarios()).contains("scenarios/appointment-scheduling.yaml");

		new ScenarioSuiteValidator(new ScenarioLoader(), new ScenarioValidator()).validate(
				suite,
				new AgentProfileLoader().load(java.nio.file.Path.of("agent-profiles/medical-receptionist-demo.yaml"))
		);
	}

	@Test
	void rejectsUnsupportedRunModeAndMissingScenario() {
		ScenarioSuite suite = new ScenarioSuite(
				"bad_suite",
				"medical_receptionist_demo",
				"retell",
				List.of("scenarios/missing.yaml")
		);

		assertThatThrownBy(() -> new ScenarioSuiteValidator(new ScenarioLoader(), new ScenarioValidator()).validate(
				suite,
				new AgentProfileLoader().load(java.nio.file.Path.of("agent-profiles/medical-receptionist-demo.yaml"))
		))
				.isInstanceOf(ScenarioSuiteValidationException.class)
				.hasMessageContaining("default_run_mode must be one of [text-chat]")
				.hasMessageContaining("scenarios[0] does not exist");
	}
}
