package com.qaai.runner;

import com.qaai.artifacts.ArtifactBundle;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.config.QaaiProperties;
import com.qaai.scenario.PatientSimulationPromptBuilder;
import com.qaai.retell.RetellClient;
import com.qaai.retell.RetellOutboundCallRequest;
import com.qaai.retell.RetellOutboundCallResponse;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import com.qaai.scenario.ScenarioValidator;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RetellCallRunner {

	private static final DateTimeFormatter CALL_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final ScenarioLoader scenarioLoader;
	private final ScenarioValidator scenarioValidator;
	private final PatientSimulationPromptBuilder patientSimulationPromptBuilder;
	private final ArtifactWriter artifactWriter;
	private final RetellClient retellClient;
	private final QaaiProperties properties;
	private final Clock clock;
	private final Supplier<String> uuidSupplier;

	@Autowired
	public RetellCallRunner(
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator,
			PatientSimulationPromptBuilder patientSimulationPromptBuilder,
			ArtifactWriter artifactWriter,
			RetellClient retellClient,
			QaaiProperties properties
	) {
		this(scenarioLoader, scenarioValidator, patientSimulationPromptBuilder, artifactWriter, retellClient,
				properties, Clock.systemDefaultZone(), () -> UUID.randomUUID().toString().substring(0, 8));
	}

	public RetellCallRunner(
			ScenarioLoader scenarioLoader,
			ScenarioValidator scenarioValidator,
			PatientSimulationPromptBuilder patientSimulationPromptBuilder,
			ArtifactWriter artifactWriter,
			RetellClient retellClient,
			QaaiProperties properties,
			Clock clock,
			Supplier<String> uuidSupplier
	) {
		this.scenarioLoader = scenarioLoader;
		this.scenarioValidator = scenarioValidator;
		this.patientSimulationPromptBuilder = patientSimulationPromptBuilder;
		this.artifactWriter = artifactWriter;
		this.retellClient = retellClient;
		this.properties = properties;
		this.clock = clock;
		this.uuidSupplier = uuidSupplier;
	}

	public ScenarioRunResult run(Path scenarioPath) {
		validateRetellConfig();

		Scenario scenario = scenarioLoader.load(scenarioPath);
		scenarioValidator.validate(scenario);
		String patientSimulationPrompt = patientSimulationPromptBuilder.build(scenario);

		OffsetDateTime startedAt = OffsetDateTime.now(clock);
		String callId = generateCallId(startedAt);
		RetellOutboundCallResponse response = retellClient.createPhoneCall(
				buildRequest(callId, scenario, patientSimulationPrompt)
		);
		String retellCallId = response == null ? null : response.callId();
		if (isBlank(retellCallId)) {
			throw new IllegalStateException("Retell create-phone-call response did not include call_id");
		}

		Path outputBaseDir = Path.of(properties.outputs().baseDir());
		Path runDirectory = outputBaseDir.resolve(callId);
		ArtifactPaths artifactPaths = new ArtifactPaths(
				runDirectory.resolve("scenario.yaml").toString(),
				runDirectory.resolve("metadata.json").toString(),
				null,
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
				"retell",
				properties.target().agentPhoneNumber(),
				retellCallId,
				startedAt,
				OffsetDateTime.now(clock),
				normalizeStatus(response.callStatus()),
				artifactPaths
		);

		ArtifactBundle artifacts = artifactWriter.writeCallStartedArtifacts(
				callId,
				scenarioPath,
				metadata,
				patientSimulationPrompt,
				buildObservations(callId, retellCallId, scenario, response)
		);

		return new ScenarioRunResult(metadata, artifacts);
	}

	private RetellOutboundCallRequest buildRequest(
			String callId,
			Scenario scenario,
			String patientSimulationPrompt
	) {
		Map<String, String> metadata = new LinkedHashMap<>();
		metadata.put("qaai_call_id", callId);
		metadata.put("scenario_id", scenario.id());
		metadata.put("workflow", scenario.workflow());
		metadata.put("run_mode", "retell");

		Map<String, String> dynamicVariables = new LinkedHashMap<>();
		dynamicVariables.put("qaai_call_id", callId);
		dynamicVariables.put("scenario_id", scenario.id());
		dynamicVariables.put("scenario_name", scenario.name());
		dynamicVariables.put("workflow", scenario.workflow());
		dynamicVariables.put("patient_name", scenario.persona().name());
		dynamicVariables.put("call_reason", scenario.goal().callReason());
		dynamicVariables.put("patient_simulation_prompt", patientSimulationPrompt);
		dynamicVariables.put("patient_date_of_birth", scenario.persona().dateOfBirth());
		dynamicVariables.put("patient_phone_number", scenario.persona().phoneNumber());
		dynamicVariables.put("goal_summary", scenario.goal().summary());
		dynamicVariables.put("expected_outcome", scenario.goal().expectedOutcome());
		dynamicVariables.put("welcome_behavior", scenario.conversationQuality().welcomeBehavior());
		dynamicVariables.put("initiative", scenario.conversationQuality().initiative());
		dynamicVariables.put("pacing", scenario.conversationQuality().pacing());
		dynamicVariables.put("clarification", scenario.conversationQuality().clarification());

		return new RetellOutboundCallRequest(
				properties.retell().fromNumber(),
				properties.target().agentPhoneNumber(),
				properties.retell().agentId(),
				metadata,
				dynamicVariables
		);
	}

	private String buildObservations(
			String callId,
			String retellCallId,
			Scenario scenario,
			RetellOutboundCallResponse response
	) {
		StringBuilder observations = new StringBuilder();
		observations.append("# Retell Call Start Observations").append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("call_id: ").append(callId).append(System.lineSeparator());
		observations.append("retell_call_id: ").append(retellCallId).append(System.lineSeparator());
		observations.append("scenario_id: ").append(scenario.id()).append(System.lineSeparator());
		observations.append("run_mode: retell").append(System.lineSeparator());
		observations.append("retell_status: ").append(response.callStatus()).append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("## Phase 3 Scope").append(System.lineSeparator());
		observations.append("- Real outbound call was created through Retell.").append(System.lineSeparator());
		observations.append("- Local metadata links this run to the Retell call id.").append(System.lineSeparator());
		observations.append("- Transcript and recording capture remain out of scope for this phase.")
				.append(System.lineSeparator());
		observations.append(System.lineSeparator());
		observations.append("## Reviewer Notes").append(System.lineSeparator());
		observations.append("- Confirm the call appears in Retell using the recorded retell_call_id.")
				.append(System.lineSeparator());

		return observations.toString();
	}

	private void validateRetellConfig() {
		requireValue(properties.retell().apiKey(), "RETELL_API_KEY");
		requireValue(properties.retell().agentId(), "RETELL_AGENT_ID");
		requireValue(properties.retell().fromNumber(), "RETELL_FROM_NUMBER");
		requireValue(properties.target().agentPhoneNumber(), "TARGET_AGENT_PHONE_NUMBER");
	}

	private void requireValue(String value, String name) {
		if (isBlank(value)) {
			throw new IllegalArgumentException(name + " is required for --run-mode=retell");
		}
	}

	private String generateCallId(OffsetDateTime startedAt) {
		return "call_" + startedAt.format(CALL_ID_TIME) + "_" + uuidSupplier.get();
	}

	private String normalizeStatus(String retellStatus) {
		if (isBlank(retellStatus)) {
			return "started";
		}
		return "retell_" + retellStatus;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
