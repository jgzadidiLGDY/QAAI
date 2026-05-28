package com.qaai.scenariogeneration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScenarioGenerationServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void generatesReviewArtifactsAndValidatesDraftScenarios() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		Path canonicalScenarios = tempDir.resolve("scenarios");
		Files.createDirectories(canonicalScenarios);
		ScenarioGenerationService service = service(outputs, request -> new ScenarioGenerationDraftSet(
				"# Coverage Plan%n%n- appointment scheduling%n".formatted(),
				List.of(validScenario(), invalidScenario())
		));

		ScenarioGenerationResult result = service.generate("medical office scheduling agent", 2);

		assertThat(result.generationId()).isEqualTo("scenario_generation_20260528_160000");
		assertThat(result.agentDescription()).hasContent("medical office scheduling agent");
		assertThat(result.coveragePlan()).hasContent("# Coverage Plan%n%n- appointment scheduling%n".formatted());
		assertThat(result.generationReportJson()).exists();
		assertThat(result.generationReportMarkdown()).exists();
		assertThat(result.generationDirectory().resolve("drafts").resolve("draft-001.yaml")).exists();
		assertThat(result.generationDirectory().resolve("drafts").resolve("draft-002.yaml")).exists();
		assertThat(Files.list(canonicalScenarios)).isEmpty();
		assertThat(Files.readString(result.generationReportJson())).contains(
				"\"provider\" : \"test-provider\"",
				"\"human_review_required\" : true",
				"\"draft_paths\"",
				"\"valid\" : true",
				"\"valid\" : false",
				"Generated drafts require human review before promotion into scenarios/.",
				"\"appointment_scheduling\" : 1",
				"\"missing_fact\" : 1"
		);
		assertThat(Files.readString(result.generationReportMarkdown())).contains(
				"# Scenario Generation Report",
				"human_review_required: true",
				"not automatically promoted into `scenarios/`",
				"coverage is required"
		);
	}

	@Test
	void requiresAgentDescription() {
		ScenarioGenerationService service = service(tempDir.resolve("outputs"), request -> new ScenarioGenerationDraftSet(
				"# Coverage Plan",
				List.of(validScenario())
		));

		assertThatThrownBy(() -> service.generate(" ", 1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("agent-description");
	}

	@Test
	void boundsScenarioCount() {
		ScenarioGenerationService service = service(tempDir.resolve("outputs"), request -> new ScenarioGenerationDraftSet(
				"# Coverage Plan",
				List.of(validScenario())
		));

		assertThatThrownBy(() -> service.generate("medical office scheduling agent", 13))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("scenario-count must be between 1 and 12");
	}

	private ScenarioGenerationService service(Path outputs, ScenarioGenerationClient client) {
		return new ScenarioGenerationService(
				outputs,
				new ObjectMapper().findAndRegisterModules(),
				new ObjectMapper(new YAMLFactory()).findAndRegisterModules(),
				new ScenarioGenerationPromptBuilder(),
				client,
				new ScenarioValidator(),
				Clock.fixed(Instant.parse("2026-05-28T16:00:00Z"), ZoneOffset.UTC)
		);
	}

	private Scenario validScenario() {
		return new Scenario(
				"generated_scheduling_missing_fact_001",
				"Generated scheduling missing fact",
				"appointment_scheduling",
				new Scenario.Persona("Alex Patient", "1980-01-01", "+15555550100"),
				new Scenario.Goal(
						"scheduling an appointment",
						"Patient needs a new appointment but lacks a preferred provider.",
						"Office gives a clear scheduling next step."
				),
				new Scenario.Constraints(
						List.of("Patient needs a routine appointment.", "Patient can attend next week."),
						List.of("Do not provide real patient data.")
				),
				new Scenario.Coverage(
						"appointment_scheduling",
						List.of("missing_fact"),
						"Check whether scheduling can proceed when one preference is unavailable."
				),
				new Scenario.ConversationQuality(
						"Start with the scheduling need.",
						"Volunteer one fact at a time.",
						"Keep turns short.",
						"Ask for rephrasing if confused.",
						List.of("Office may not give a concrete next step.")
				),
				List.of(new Scenario.Step("greeting", "Hi, I need to schedule an appointment."))
		);
	}

	private Scenario invalidScenario() {
		return new Scenario(
				"generated_invalid_001",
				"Generated invalid",
				"appointment_scheduling",
				new Scenario.Persona("Jordan Patient", "1985-01-01", "+15555550101"),
				new Scenario.Goal("scheduling", "Needs scheduling.", "Gets a next step."),
				new Scenario.Constraints(List.of("Synthetic fact."), List.of("Do not provide real patient data.")),
				null,
				new Scenario.ConversationQuality(
						"Start clearly.",
						"Do not overshare.",
						"Use short turns.",
						"Clarify confusion.",
						List.of("Office may transfer.")
				),
				List.of(new Scenario.Step("greeting", "Hi, I need an appointment."))
		);
	}

	private interface ScenarioGenerationClient extends com.qaai.scenariogeneration.ScenarioGenerationClient {
		@Override
		ScenarioGenerationDraftSet generate(ScenarioGenerationRequest request);

		@Override
		default String provider() {
			return "test-provider";
		}

		@Override
		default String model() {
			return "test-model";
		}
	}
}
