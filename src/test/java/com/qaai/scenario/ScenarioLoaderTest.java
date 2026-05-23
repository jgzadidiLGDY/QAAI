package com.qaai.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScenarioLoaderTest {

	private final ScenarioLoader loader = new ScenarioLoader();

	@Test
	void loadsScenarioYaml() {
		Scenario scenario = loader.load(Path.of("scenarios/appointment-reschedule.yaml"));

		assertThat(scenario.id()).isEqualTo("appointment_reschedule_001");
		assertThat(scenario.name()).isEqualTo("Appointment reschedule");
		assertThat(scenario.workflow()).isEqualTo("appointment_rescheduling");
		assertThat(scenario.persona().name()).isEqualTo("Maria Lopez");
		assertThat(scenario.persona().dateOfBirth()).isEqualTo("1982-04-19");
		assertThat(scenario.goal().summary()).isEqualTo("Reschedule an existing appointment to next week.");
		assertThat(scenario.steps()).hasSize(3);
		assertThat(scenario.steps().getFirst().patientSays()).isEqualTo("Hi, I need to reschedule my appointment.");
	}
}
