package com.qaai.runner;

import com.qaai.artifacts.ArtifactBundle;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.RuntimeReproducibilityMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.config.QaaiProperties;
import com.qaai.scenario.PatientSimulationPromptBuilder;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextChatRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(TextChatRunner.class);

	private static final DateTimeFormatter CALL_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final ScenarioLoader scenarioLoader;
	private final ScenarioValidator scenarioValidator;
	private final PatientSimulationPromptBuilder patientSimulationPromptBuilder;
	private final ArtifactWriter artifactWriter;
	private final QaaiProperties properties;
	private final Clock clock;
	private final Supplier<String> uuidSupplier;

	@Autowired
	public TextChatRunner(
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

	public TextChatRunner(
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
		LOGGER.info("Starting text chat run for scenario_path={}", scenarioPath);
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
				runDirectory.resolve("transcript.json").toString(),
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
				"text_chat",
				"text",
				null,
				null,
				startedAt,
				OffsetDateTime.now(clock),
				"completed",
				artifactPaths
		);
		NormalizedTranscript transcript = buildTranscript(callId, scenario);

		ArtifactBundle artifacts = artifactWriter.writeTextChatArtifacts(
				callId,
				scenarioPath,
				metadata,
				patientSimulationPrompt,
				transcript,
				renderTranscript(metadata, scenario, transcript),
				buildObservations(callId, scenario)
		);

		LOGGER.info("Completed text chat run call_id={} scenario_id={} artifacts={}", callId, scenario.id(),
				artifacts.runDirectory());
		return new ScenarioRunResult(metadata, artifacts);
	}

	private String generateCallId(OffsetDateTime startedAt) {
		return "call_" + startedAt.format(CALL_ID_TIME) + "_" + uuidSupplier.get();
	}

	private NormalizedTranscript buildTranscript(String callId, Scenario scenario) {
		List<TranscriptTurn> turns = new ArrayList<>();
		for (int index = 0; index < scenario.steps().size(); index++) {
			Scenario.Step step = scenario.steps().get(index);
			turns.add(new TranscriptTurn(index + 1, "patient", step.patientSays(), null));
		}
		return new NormalizedTranscript(callId, scenario.id(), "text_chat", List.copyOf(turns));
	}

	private String renderTranscript(RunMetadata metadata, Scenario scenario, NormalizedTranscript transcript) {
		StringBuilder text = new StringBuilder();
		text.append("Text Chat Transcript").append(System.lineSeparator());
		text.append("call_id: ").append(metadata.callId()).append(System.lineSeparator());
		text.append("scenario_id: ").append(scenario.id()).append(System.lineSeparator());
		text.append("workflow: ").append(scenario.workflow()).append(System.lineSeparator());
		text.append("channel: text").append(System.lineSeparator());
		text.append("source: text_chat").append(System.lineSeparator());
		text.append(System.lineSeparator());
		text.append("Patient Turns").append(System.lineSeparator());
		for (TranscriptTurn turn : transcript.turns()) {
			text.append(turn.index())
					.append(". [")
					.append(turn.speaker())
					.append("] ")
					.append(turn.text())
					.append(System.lineSeparator());
		}
		return text.toString();
	}

	private String buildObservations(String callId, Scenario scenario) {
		StringBuilder observations = new StringBuilder();
		observations.append("# Conversation Quality Observations").append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("call_id: ").append(callId).append(System.lineSeparator());
		observations.append("scenario_id: ").append(scenario.id()).append(System.lineSeparator());
		observations.append("run_mode: text_chat").append(System.lineSeparator());
		observations.append("channel: text").append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("## Before").append(System.lineSeparator());
		observations.append("- Scenario includes deterministic patient turns for a local text interaction.")
				.append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("## After").append(System.lineSeparator());
		observations.append("- Text chat transcript is available as normalized evidence for review.")
				.append(System.lineSeparator());
		observations.append("- Human reviewer should compare downstream agent behavior against these expected risks:")
				.append(System.lineSeparator());
		for (String risk : scenario.conversationQuality().expectedRisks()) {
			observations.append("  - ").append(risk).append(System.lineSeparator());
		}
		observations.append(System.lineSeparator());
		observations.append("## Reviewer Notes").append(System.lineSeparator());
		observations.append("- Pending human review after text interaction evidence is inspected.")
				.append(System.lineSeparator());

		return observations.toString();
	}
}
