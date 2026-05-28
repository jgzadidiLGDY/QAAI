package com.qaai.scenariogeneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.qaai.artifacts.ArtifactWriteException;
import com.qaai.config.QaaiProperties;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioValidationException;
import com.qaai.scenario.ScenarioValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScenarioGenerationService {

	private static final int DEFAULT_SCENARIO_COUNT = 5;
	private static final int MAX_SCENARIO_COUNT = 12;

	private final Path outputBaseDir;
	private final ObjectMapper jsonMapper;
	private final ObjectMapper yamlMapper;
	private final ScenarioGenerationPromptBuilder promptBuilder;
	private final ScenarioGenerationClient client;
	private final ScenarioValidator validator;
	private final Clock clock;

	@Autowired
	public ScenarioGenerationService(
			QaaiProperties properties,
			ObjectMapper objectMapper,
			ScenarioGenerationPromptBuilder promptBuilder,
			ScenarioGenerationClient client,
			ScenarioValidator validator
	) {
		this(
				Path.of(properties.outputs().baseDir()),
				objectMapper.findAndRegisterModules(),
				new ObjectMapper(new YAMLFactory()).findAndRegisterModules(),
				promptBuilder,
				client,
				validator,
				Clock.systemDefaultZone()
		);
	}

	public ScenarioGenerationService(
			Path outputBaseDir,
			ObjectMapper jsonMapper,
			ObjectMapper yamlMapper,
			ScenarioGenerationPromptBuilder promptBuilder,
			ScenarioGenerationClient client,
			ScenarioValidator validator,
			Clock clock
	) {
		this.outputBaseDir = outputBaseDir;
		this.jsonMapper = jsonMapper;
		this.yamlMapper = yamlMapper;
		this.promptBuilder = promptBuilder;
		this.client = client;
		this.validator = validator;
		this.clock = clock;
	}

	public ScenarioGenerationResult generate(String agentDescription, Integer requestedScenarioCount) {
		if (agentDescription == null || agentDescription.isBlank()) {
			throw new IllegalArgumentException("Provide --agent-description=<description> with --generate-scenarios");
		}
		int scenarioCount = scenarioCount(requestedScenarioCount);
		OffsetDateTime generatedAt = OffsetDateTime.now(clock);
		String generationId = "scenario_generation_"
				+ generatedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String prompt = promptBuilder.build(agentDescription, scenarioCount);
		ScenarioGenerationDraftSet draftSet = client.generate(new ScenarioGenerationRequest(
				generationId,
				agentDescription,
				scenarioCount,
				prompt
		));
		return writeArtifacts(generationId, generatedAt, agentDescription, draftSet);
	}

	private ScenarioGenerationResult writeArtifacts(
			String generationId,
			OffsetDateTime generatedAt,
			String agentDescription,
			ScenarioGenerationDraftSet draftSet
	) {
		Path generationDirectory = outputBaseDir.resolve("scenario-generation").resolve(generationId);
		Path draftsDirectory = generationDirectory.resolve("drafts");
		Path agentDescriptionPath = generationDirectory.resolve("agent-description.md");
		Path coveragePlanPath = generationDirectory.resolve("coverage-plan.md");
		Path reportJsonPath = generationDirectory.resolve("generation-report.json");
		Path reportMarkdownPath = generationDirectory.resolve("generation-report.md");
		List<Scenario> scenarios = draftSet == null || draftSet.scenarios() == null
				? List.of()
				: draftSet.scenarios();
		List<String> draftPaths = new ArrayList<>();
		List<ScenarioDraftValidationResult> validationResults = new ArrayList<>();

		try {
			Files.createDirectories(draftsDirectory);
			Files.writeString(agentDescriptionPath, agentDescription);
			Files.writeString(coveragePlanPath, coveragePlan(draftSet));
			for (int index = 0; index < scenarios.size(); index++) {
				Scenario scenario = scenarios.get(index);
				Path draftPath = draftsDirectory.resolve("draft-%03d.yaml".formatted(index + 1));
				yamlMapper.writeValue(draftPath.toFile(), scenario);
				draftPaths.add(draftPath.toString());
				validationResults.add(validateDraft(draftPath, scenario));
			}
			ScenarioGenerationReport report = report(
					generationId,
					generatedAt,
					agentDescription,
					draftPaths,
					validationResults,
					scenarios
			);
			jsonMapper.writerWithDefaultPrettyPrinter().writeValue(reportJsonPath.toFile(), report);
			Files.writeString(reportMarkdownPath, renderMarkdown(report));
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write scenario generation artifacts: " + generationId,
					exception);
		}

		return new ScenarioGenerationResult(
				generationId,
				generationDirectory,
				agentDescriptionPath,
				coveragePlanPath,
				reportJsonPath,
				reportMarkdownPath
		);
	}

	private ScenarioDraftValidationResult validateDraft(Path draftPath, Scenario scenario) {
		try {
			validator.validate(scenario);
			return new ScenarioDraftValidationResult(draftPath.toString(), scenario.id(), true, List.of());
		} catch (ScenarioValidationException exception) {
			return new ScenarioDraftValidationResult(
					draftPath.toString(),
					scenario == null ? null : scenario.id(),
					false,
					exception.errors()
			);
		}
	}

	private ScenarioGenerationReport report(
			String generationId,
			OffsetDateTime generatedAt,
			String agentDescription,
			List<String> draftPaths,
			List<ScenarioDraftValidationResult> validationResults,
			List<Scenario> scenarios
	) {
		List<String> warnings = new ArrayList<>();
		if (draftPaths.isEmpty()) {
			warnings.add("No scenario drafts were generated.");
		}
		if (validationResults.stream().anyMatch(result -> !result.valid())) {
			warnings.add("One or more scenario drafts failed deterministic validation.");
		}
		warnings.add("Generated drafts require human review before promotion into scenarios/.");
		return new ScenarioGenerationReport(
				generationId,
				generatedAt,
				agentDescription,
				client.provider(),
				client.model(),
				true,
				List.copyOf(draftPaths),
				List.copyOf(validationResults),
				coverageByWorkflow(scenarios),
				coverageByEdgeCase(scenarios),
				List.copyOf(warnings)
		);
	}

	private String renderMarkdown(ScenarioGenerationReport report) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# Scenario Generation Report%n%n".formatted());
		markdown.append("generation_id: ").append(report.generationId()).append("%n".formatted());
		markdown.append("provider: ").append(report.provider()).append("%n".formatted());
		markdown.append("model: ").append(report.model()).append("%n".formatted());
		markdown.append("human_review_required: true%n%n".formatted());
		markdown.append("Generated drafts are review artifacts and are not automatically promoted into `scenarios/`.%n%n"
				.formatted());
		markdown.append("## Draft Validation%n%n".formatted());
		if (report.validationResults().isEmpty()) {
			markdown.append("No drafts generated.%n%n".formatted());
		} else {
			for (ScenarioDraftValidationResult result : report.validationResults()) {
				markdown.append("- ").append(result.draftPath()).append(": ")
						.append(result.valid() ? "valid" : "invalid").append("%n".formatted());
				for (String error : result.errors()) {
					markdown.append("  - ").append(error).append("%n".formatted());
				}
			}
			markdown.append("%n".formatted());
		}
		markdown.append("## Coverage By Workflow%n%n".formatted());
		appendCounts(markdown, report.coverageByWorkflow());
		markdown.append("## Coverage By Edge Case%n%n".formatted());
		appendCounts(markdown, report.coverageByEdgeCase());
		markdown.append("## Warnings%n%n".formatted());
		for (String warning : report.warnings()) {
			markdown.append("- ").append(warning).append("%n".formatted());
		}
		return markdown.toString();
	}

	private void appendCounts(StringBuilder markdown, Map<String, Long> counts) {
		if (counts.isEmpty()) {
			markdown.append("No coverage entries.%n%n".formatted());
			return;
		}
		for (Map.Entry<String, Long> entry : counts.entrySet()) {
			markdown.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("%n"
					.formatted());
		}
		markdown.append("%n".formatted());
	}

	private String coveragePlan(ScenarioGenerationDraftSet draftSet) {
		if (draftSet == null || draftSet.coveragePlanMarkdown() == null || draftSet.coveragePlanMarkdown().isBlank()) {
			return "# Coverage Plan%n%nNo coverage plan was returned by the scenario generator.%n".formatted();
		}
		return draftSet.coveragePlanMarkdown();
	}

	private Map<String, Long> coverageByWorkflow(List<Scenario> scenarios) {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (Scenario scenario : scenarios) {
			if (scenario == null || scenario.coverage() == null
					|| scenario.coverage().workflowArea() == null
					|| scenario.coverage().workflowArea().isBlank()) {
				continue;
			}
			counts.put(scenario.coverage().workflowArea(), counts.getOrDefault(scenario.coverage().workflowArea(), 0L) + 1);
		}
		return counts;
	}

	private Map<String, Long> coverageByEdgeCase(List<Scenario> scenarios) {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (Scenario scenario : scenarios) {
			if (scenario == null || scenario.coverage() == null || scenario.coverage().edgeCases() == null) {
				continue;
			}
			for (String edgeCase : scenario.coverage().edgeCases()) {
				if (edgeCase != null && !edgeCase.isBlank()) {
					counts.put(edgeCase, counts.getOrDefault(edgeCase, 0L) + 1);
				}
			}
		}
		return counts;
	}

	private int scenarioCount(Integer requestedScenarioCount) {
		if (requestedScenarioCount == null) {
			return DEFAULT_SCENARIO_COUNT;
		}
		if (requestedScenarioCount < 1 || requestedScenarioCount > MAX_SCENARIO_COUNT) {
			throw new IllegalArgumentException("scenario-count must be between 1 and " + MAX_SCENARIO_COUNT);
		}
		return requestedScenarioCount;
	}
}
