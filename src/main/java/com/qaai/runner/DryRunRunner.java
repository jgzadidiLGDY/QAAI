package com.qaai.runner;

import com.qaai.artifacts.ArtifactBundle;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.config.QaaiProperties;
import com.qaai.scenario.PatientSimulationPromptBuilder;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DryRunRunner {

	private static final DateTimeFormatter CALL_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final ScenarioLoader scenarioLoader;
	private final ScenarioValidator scenarioValidator;
	private final PatientSimulationPromptBuilder patientSimulationPromptBuilder;
	private final ArtifactWriter artifactWriter;
	private final QaaiProperties properties;
	private final Clock clock;
	private final Supplier<String> uuidSupplier;

	@Autowired
	public DryRunRunner(
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator,
			PatientSimulationPromptBuilder patientSimulationPromptBuilder,
			ArtifactWriter artifactWriter,
			QaaiProperties properties
	) {
		this(scenarioLoader, scenarioValidator, patientSimulationPromptBuilder, artifactWriter, properties,
				Clock.systemDefaultZone(),
				() -> UUID.randomUUID().toString().substring(0, 8));
	}

	public DryRunRunner(
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator,
			PatientSimulationPromptBuilder patientSimulationPromptBuilder,
			ArtifactWriter artifactWriter,
			QaaiProperties properties,
			Clock clock,
			Supplier<String> uuidSupplier
	) {
		this.scenarioLoader = scenarioLoader;
		this.scenarioValidator = scenarioValidator;
		this.patientSimulationPromptBuilder = patientSimulationPromptBuilder;
		this.artifactWriter = artifactWriter;
		this.properties = properties;
		this.clock = clock;
		this.uuidSupplier = uuidSupplier;
	}

	public ScenarioRunResult run(Path scenarioPath) {
		Scenario scenario = scenarioLoader.load(scenarioPath);
		scenarioValidator.validate(scenario);
		String patientSimulationPrompt = patientSimulationPromptBuilder.build(scenario);

		OffsetDateTime startedAt = OffsetDateTime.now(clock);
		String callId = generateCallId(startedAt);
		Path outputBaseDir = Path.of(properties.outputs().baseDir());
		Path runDirectory = outputBaseDir.resolve(callId);

		ArtifactPaths artifactPaths = new ArtifactPaths(
				runDirectory.resolve("scenario.yaml").toString(),
				runDirectory.resolve("metadata.json").toString(),
				runDirectory.resolve("transcript.txt").toString(),
				null,
				runDirectory.resolve("patient_simulation.md").toString(),
				null,
				null,
				null,
				null,
				runDirectory.resolve("observations.md").toString()
		);
		RunMetadata metadata = new RunMetadata(
				callId,
				scenario.id(),
				"dry_run",
				properties.target().agentPhoneNumber(),
				null,
				startedAt,
				OffsetDateTime.now(clock),
				"completed",
				artifactPaths
		);

		ArtifactBundle artifacts = artifactWriter.writeDryRunArtifacts(
				callId,
				scenarioPath,
				metadata,
				patientSimulationPrompt,
				buildTranscript(callId, scenario),
				buildObservations(callId, scenario)
		);

		return new ScenarioRunResult(metadata, artifacts);
	}

	private String generateCallId(OffsetDateTime startedAt) {
		return "call_" + startedAt.format(CALL_ID_TIME) + "_" + uuidSupplier.get();
	}

	private String buildTranscript(String callId, Scenario scenario) {
		StringBuilder transcript = new StringBuilder();
		transcript.append("Dry Run Transcript").append(System.lineSeparator());
		transcript.append("call_id: ").append(callId).append(System.lineSeparator());
		transcript.append("scenario_id: ").append(scenario.id()).append(System.lineSeparator());
		transcript.append("workflow: ").append(scenario.workflow()).append(System.lineSeparator());
		transcript.append("source: dry_run").append(System.lineSeparator());
		transcript.append(System.lineSeparator());
		transcript.append("Conversation Quality Guidance").append(System.lineSeparator());
		transcript.append("welcome_behavior: ")
				.append(scenario.conversationQuality().welcomeBehavior())
				.append(System.lineSeparator());
		transcript.append("initiative: ")
				.append(scenario.conversationQuality().initiative())
				.append(System.lineSeparator());
		transcript.append("pacing: ")
				.append(scenario.conversationQuality().pacing())
				.append(System.lineSeparator());
		transcript.append("clarification: ")
				.append(scenario.conversationQuality().clarification())
				.append(System.lineSeparator());
		transcript.append(System.lineSeparator());
		transcript.append("Expected Conversation Risks").append(System.lineSeparator());
		for (String risk : scenario.conversationQuality().expectedRisks()) {
			transcript.append("- ").append(risk).append(System.lineSeparator());
		}
		transcript.append(System.lineSeparator());
		transcript.append("Patient Turns").append(System.lineSeparator());

		for (int index = 0; index < scenario.steps().size(); index++) {
			Scenario.Step step = scenario.steps().get(index);
			transcript.append(index + 1)
					.append(". [patient] ")
					.append(step.patientSays())
					.append(" (intent: ")
					.append(step.intent())
					.append(")")
					.append(System.lineSeparator());
		}

		return transcript.toString();
	}

	private String buildObservations(String callId, Scenario scenario) {
		StringBuilder observations = new StringBuilder();
		observations.append("# Conversation Quality Observations").append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("call_id: ").append(callId).append(System.lineSeparator());
		observations.append("scenario_id: ").append(scenario.id()).append(System.lineSeparator());
		observations.append("run_mode: dry_run").append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("## Before").append(System.lineSeparator());
		observations.append("- Scenario includes deterministic patient turns from Phase 1.").append(System.lineSeparator());
		observations.append("- Conversation-quality guidance is now explicit in the scenario snapshot.").append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("## After").append(System.lineSeparator());
		observations.append("- Dry-run transcript includes welcome, initiative, pacing, and clarification guidance.")
				.append(System.lineSeparator());
		observations.append("- Human reviewer should compare real future calls against these expected risks:")
				.append(System.lineSeparator());
		for (String risk : scenario.conversationQuality().expectedRisks()) {
			observations.append("  - ").append(risk).append(System.lineSeparator());
		}
		observations.append(System.lineSeparator());
		observations.append("## Reviewer Notes").append(System.lineSeparator());
		observations.append("- Pending human review after a real call artifact exists.").append(System.lineSeparator());

		return observations.toString();
	}
}
