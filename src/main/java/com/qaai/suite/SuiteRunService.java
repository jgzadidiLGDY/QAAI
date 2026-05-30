package com.qaai.suite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.agent.AgentProfile;
import com.qaai.agent.AgentProfileLoader;
import com.qaai.agent.AgentProfileValidator;
import com.qaai.artifacts.ArtifactCompleteness;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.config.QaaiProperties;
import com.qaai.runner.RunContext;
import com.qaai.runner.ScenarioRunResult;
import com.qaai.runner.TextChatRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SuiteRunService {

	private static final DateTimeFormatter SUITE_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final ScenarioSuiteLoader suiteLoader;
	private final ScenarioSuiteValidator suiteValidator;
	private final AgentProfileLoader profileLoader;
	private final AgentProfileValidator profileValidator;
	private final TextChatRunner textChatRunner;
	private final ArtifactCompletenessChecker completenessChecker;
	private final ObjectMapper objectMapper;
	private final Path outputBaseDir;
	private final Path profileBaseDir;
	private final Clock clock;
	private final Supplier<String> uuidSupplier;

	@Autowired
	public SuiteRunService(
			ScenarioSuiteLoader suiteLoader,
			ScenarioSuiteValidator suiteValidator,
			AgentProfileLoader profileLoader,
			AgentProfileValidator profileValidator,
			TextChatRunner textChatRunner,
			ArtifactCompletenessChecker completenessChecker,
			ObjectMapper objectMapper,
			QaaiProperties properties
	) {
		this(suiteLoader, suiteValidator, profileLoader, profileValidator, textChatRunner, completenessChecker,
				objectMapper, Path.of(properties.outputs().baseDir()), Path.of("agent-profiles"),
				Clock.systemDefaultZone(), () -> UUID.randomUUID().toString().substring(0, 8));
	}

	public SuiteRunService(
			ScenarioSuiteLoader suiteLoader,
			ScenarioSuiteValidator suiteValidator,
			AgentProfileLoader profileLoader,
			AgentProfileValidator profileValidator,
			TextChatRunner textChatRunner,
			ArtifactCompletenessChecker completenessChecker,
			ObjectMapper objectMapper,
			Path outputBaseDir,
			Path profileBaseDir,
			Clock clock,
			Supplier<String> uuidSupplier
	) {
		this.suiteLoader = suiteLoader;
		this.suiteValidator = suiteValidator;
		this.profileLoader = profileLoader;
		this.profileValidator = profileValidator;
		this.textChatRunner = textChatRunner;
		this.completenessChecker = completenessChecker;
		this.objectMapper = objectMapper.findAndRegisterModules();
		this.outputBaseDir = outputBaseDir;
		this.profileBaseDir = profileBaseDir;
		this.clock = clock;
		this.uuidSupplier = uuidSupplier;
	}

	public SuiteRunResult run(Path suitePath) {
		ScenarioSuite suite = suiteLoader.load(suitePath);
		if (suite.agentProfile() == null || suite.agentProfile().isBlank()) {
			throw new ScenarioSuiteValidationException(List.of("agent_profile is required"));
		}
		Path profilePath = resolveProfilePath(suite.agentProfile());
		AgentProfile profile = profileLoader.load(profilePath);
		profileValidator.validate(profile);
		suiteValidator.validate(suite, profile);

		String suiteRunId = generateSuiteRunId(OffsetDateTime.now(clock));
		Path suiteDirectory = outputBaseDir.resolve("suites").resolve(suiteRunId);
		RunContext runContext = new RunContext(profile.id(), suite.id(), suiteRunId);
		List<SuiteRunReport.SuiteScenarioRunSummary> summaries = new ArrayList<>();

		for (String scenarioPath : suite.scenarios()) {
			ScenarioRunResult result = runScenario(suite.defaultRunMode(), Path.of(scenarioPath), runContext);
			ArtifactCompleteness completeness = completenessChecker.check(result.metadata());
			summaries.add(new SuiteRunReport.SuiteScenarioRunSummary(
					scenarioPath,
					result.metadata().scenarioId(),
					result.metadata().callId(),
					result.metadata().status(),
					completeness.complete(),
					result.metadata().artifactPaths().metadata(),
					result.metadata().artifactPaths().transcriptText(),
					result.metadata().artifactPaths().transcriptJson(),
					completeness.warnings()
			));
		}

		SuiteRunReport report = new SuiteRunReport(
				suiteRunId,
				suite.id(),
				profile.id(),
				OffsetDateTime.now(clock),
				suite.defaultRunMode(),
				true,
				List.copyOf(summaries),
				List.of()
		);
		return writeSuiteArtifacts(suitePath, profilePath, suiteDirectory, report);
	}

	private ScenarioRunResult runScenario(String runMode, Path scenarioPath, RunContext runContext) {
		if ("text-chat".equals(runMode)) {
			return textChatRunner.run(scenarioPath, runContext);
		}
		throw new ScenarioSuiteValidationException(List.of("default_run_mode must be one of [text-chat]"));
	}

	private Path resolveProfilePath(String agentProfileId) {
		Path exactPath = profileBaseDir.resolve(agentProfileId + ".yaml");
		if (Files.exists(exactPath)) {
			return exactPath;
		}
		return profileBaseDir.resolve(agentProfileId.replace("_", "-") + ".yaml");
	}

	private SuiteRunResult writeSuiteArtifacts(
			Path suitePath,
			Path profilePath,
			Path suiteDirectory,
			SuiteRunReport report
	) {
		Path suiteSnapshot = suiteDirectory.resolve("suite.yaml");
		Path profileSnapshot = suiteDirectory.resolve("agent-profile.yaml");
		Path reportJson = suiteDirectory.resolve("suite-report.json");
		Path reportMarkdown = suiteDirectory.resolve("suite-report.md");

		try {
			Files.createDirectories(suiteDirectory);
			Files.copy(suitePath, suiteSnapshot, StandardCopyOption.REPLACE_EXISTING);
			Files.copy(profilePath, profileSnapshot, StandardCopyOption.REPLACE_EXISTING);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportJson.toFile(), report);
			Files.writeString(reportMarkdown, renderMarkdown(report));
			return new SuiteRunResult(report.suiteRunId(), suiteDirectory, suiteSnapshot, profileSnapshot,
					reportJson, reportMarkdown);
		} catch (IOException exception) {
			throw new ScenarioSuiteLoadException("Unable to write suite artifacts: " + suiteDirectory, exception);
		}
	}

	private String renderMarkdown(SuiteRunReport report) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# Suite Run Report").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("suite_run_id: ").append(report.suiteRunId()).append(System.lineSeparator());
		markdown.append("suite_id: ").append(report.suiteId()).append(System.lineSeparator());
		markdown.append("agent_profile_id: ").append(report.agentProfileId()).append(System.lineSeparator());
		markdown.append("run_mode: ").append(report.runMode()).append(System.lineSeparator());
		markdown.append("human_review_required: true").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("## Scenario Runs").append(System.lineSeparator());
		for (SuiteRunReport.SuiteScenarioRunSummary run : report.runs()) {
			markdown.append("- ")
					.append(run.scenarioId())
					.append(" -> ")
					.append(run.callId())
					.append(" (")
					.append(run.status())
					.append(", complete=")
					.append(run.complete())
					.append(")")
					.append(System.lineSeparator());
			markdown.append("  - metadata: ").append(run.metadataPath()).append(System.lineSeparator());
			markdown.append("  - transcript_json: ").append(run.transcriptJsonPath()).append(System.lineSeparator());
		}
		markdown.append(System.lineSeparator());
		markdown.append("## Notes").append(System.lineSeparator());
		markdown.append("- Suite reports summarize artifacts for human review and do not decide pass/fail.")
				.append(System.lineSeparator());
		return markdown.toString();
	}

	private String generateSuiteRunId(OffsetDateTime generatedAt) {
		return "suite_" + generatedAt.format(SUITE_ID_TIME) + "_" + uuidSupplier.get();
	}
}
