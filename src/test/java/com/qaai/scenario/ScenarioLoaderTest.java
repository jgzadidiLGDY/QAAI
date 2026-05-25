package com.qaai.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
		assertThat(scenario.goal().callReason()).isEqualTo("rescheduling my appointment");
		assertThat(scenario.goal().summary()).isEqualTo("Reschedule an existing appointment to next week.");
		assertThat(scenario.conversationQuality().welcomeBehavior())
				.isEqualTo("Wait briefly for the agent greeting, then clearly state the rescheduling need.");
		assertThat(scenario.conversationQuality().expectedRisks())
				.contains("Agent may skip confirmation of the new appointment time.");
		assertThat(scenario.steps()).hasSize(3);
		assertThat(scenario.steps().getFirst().patientSays()).isEqualTo("Hi, I need to reschedule my appointment.");
	}

	@ParameterizedTest
	@MethodSource("scenarioPaths")
	void loadsAndValidatesAllScenarioYamlFiles(Path scenarioPath) {
		Scenario scenario = loader.load(scenarioPath);

		new ScenarioValidator().validate(scenario);
	}

	private static Stream<Path> scenarioPaths() {
		return Stream.of(
				Path.of("scenarios/appointment-reschedule.yaml"),
				Path.of("scenarios/appointment-scheduling.yaml"),
				Path.of("scenarios/prescription-refill.yaml"),
				Path.of("scenarios/billing-question.yaml"),
				Path.of("scenarios/insurance-verification.yaml")
		);
	}
}
