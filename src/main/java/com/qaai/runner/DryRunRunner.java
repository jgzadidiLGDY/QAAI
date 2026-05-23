package com.qaai.runner;

import com.qaai.artifacts.ArtifactBundle;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.config.QaaiProperties;
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
	private final ArtifactWriter artifactWriter;
	private final QaaiProperties properties;
	private final Clock clock;
	private final Supplier<String> uuidSupplier;

	@Autowired
	public DryRunRunner(
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator,
			ArtifactWriter artifactWriter,
			QaaiProperties properties
	) {
		this(scenarioLoader, scenarioValidator, artifactWriter, properties, Clock.systemDefaultZone(),
				() -> UUID.randomUUID().toString().substring(0, 8));
	}

	public DryRunRunner(
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator,
			ArtifactWriter artifactWriter,
			QaaiProperties properties,
			Clock clock,
			Supplier<String> uuidSupplier
	) {
		this.scenarioLoader = scenarioLoader;
		this.scenarioValidator = scenarioValidator;
		this.artifactWriter = artifactWriter;
		this.properties = properties;
		this.clock = clock;
		this.uuidSupplier = uuidSupplier;
	}

	public DryRunResult run(Path scenarioPath) {
		Scenario scenario = scenarioLoader.load(scenarioPath);
		scenarioValidator.validate(scenario);

		OffsetDateTime startedAt = OffsetDateTime.now(clock);
		String callId = generateCallId(startedAt);
		Path outputBaseDir = Path.of(properties.outputs().baseDir());
		Path runDirectory = outputBaseDir.resolve(callId);

		ArtifactPaths artifactPaths = new ArtifactPaths(
				runDirectory.resolve("scenario.yaml").toString(),
				runDirectory.resolve("metadata.json").toString(),
				runDirectory.resolve("transcript.txt").toString()
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
				buildTranscript(callId, scenario)
		);

		return new DryRunResult(metadata, artifacts);
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
}
