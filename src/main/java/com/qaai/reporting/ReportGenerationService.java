package com.qaai.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.analysis.AnalysisFinding;
import com.qaai.analysis.AnalysisReport;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriteException;
import com.qaai.artifacts.RunIndexEntry;
import com.qaai.artifacts.RunIndexWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.config.QaaiProperties;
import com.qaai.evaluation.EvaluationDimensionResult;
import com.qaai.evaluation.EvaluationReport;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportGenerationService {

	private final ObjectMapper objectMapper;
	private final Path outputBaseDir;
	private final Path scenarioDirectory;
	private final RunIndexWriter runIndexWriter;
	private final ArtifactCompletenessChecker completenessChecker;
	private final ScenarioLoader scenarioLoader;
	private final StaticReportRenderer renderer;
	private final Clock clock;

	@Autowired
	public ReportGenerationService(
			ObjectMapper objectMapper,
			QaaiProperties properties,
			ScenarioLoader scenarioLoader,
			StaticReportRenderer renderer
	) {
		this(
				objectMapper,
				Path.of(properties.outputs().baseDir()),
				Path.of("scenarios"),
				new RunIndexWriter(objectMapper, Path.of(properties.outputs().baseDir()),
						new ArtifactCompletenessChecker()),
				new ArtifactCompletenessChecker(),
				scenarioLoader,
				renderer,
				Clock.systemDefaultZone()
		);
	}

	public ReportGenerationService(
			ObjectMapper objectMapper,
			Path outputBaseDir,
			Path scenarioDirectory,
			RunIndexWriter runIndexWriter,
			ArtifactCompletenessChecker completenessChecker,
			ScenarioLoader scenarioLoader,
			StaticReportRenderer renderer,
			Clock clock
	) {
		this.objectMapper = objectMapper.findAndRegisterModules();
		this.outputBaseDir = outputBaseDir;
		this.scenarioDirectory = scenarioDirectory;
		this.runIndexWriter = runIndexWriter;
		this.completenessChecker = completenessChecker;
		this.scenarioLoader = scenarioLoader;
		this.renderer = renderer;
		this.clock = clock;
	}

	public ReportResult generate() {
		OffsetDateTime generatedAt = OffsetDateTime.now(clock);
		String reportId = "report_" + generatedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		ReportModel report = buildReport(reportId, generatedAt);
		Path reportDirectory = outputBaseDir.resolve("reports").resolve(reportId);
		Path reportJson = reportDirectory.resolve("report.json");
		Path reportMarkdown = reportDirectory.resolve("report.md");
		Path reportHtml = reportDirectory.resolve("index.html");

		try {
			Files.createDirectories(reportDirectory);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportJson.toFile(), report);
			Files.writeString(reportMarkdown, renderer.renderMarkdown(report));
			Files.writeString(reportHtml, renderer.renderHtml(report));
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write QA static report: " + reportId, exception);
		}

		return new ReportResult(reportId, reportDirectory, reportJson, reportMarkdown, reportHtml);
	}

	private ReportModel buildReport(String reportId, OffsetDateTime generatedAt) {
		List<RunMetadata> metadata = latestMetadata();
		return new ReportModel(
				reportId,
				generatedAt,
				true,
				runSummaries(metadata),
				evaluationSummaries(metadata),
				severityCounts(metadata),
				coverageSummaries()
		);
	}

	private List<RunMetadata> latestMetadata() {
		Map<String, RunIndexEntry> latestEntries = new LinkedHashMap<>();
		for (RunIndexEntry entry : runIndexWriter.readAll()) {
			latestEntries.put(entry.callId(), entry);
		}
		List<RunMetadata> metadata = new ArrayList<>();
		for (RunIndexEntry entry : latestEntries.values()) {
			Path metadataPath = pathOrNull(entry.metadataPath());
			if (metadataPath == null || !Files.exists(metadataPath)) {
				continue;
			}
			metadata.add(read(metadataPath, RunMetadata.class));
		}
		metadata.sort(Comparator.comparing(RunMetadata::startedAt, Comparator.nullsLast(Comparator.naturalOrder())));
		return metadata;
	}

	private List<ReportRunSummary> runSummaries(List<RunMetadata> metadata) {
		return metadata.stream()
				.map(run -> {
					ArtifactPaths paths = run.artifactPaths();
					return new ReportRunSummary(
							run.callId(),
							run.scenarioId(),
							run.runMode(),
							run.channel(),
							run.status(),
							completenessChecker.check(run).complete(),
							completenessChecker.check(run).warnings(),
							paths.metadata(),
							paths.transcriptText(),
							paths.analysisMarkdown() == null ? paths.analysisJson() : paths.analysisMarkdown(),
							paths.evaluationMarkdown() == null ? paths.evaluationJson() : paths.evaluationMarkdown()
					);
				})
				.toList();
	}

	private Map<String, ReportEvaluationSummary> evaluationSummaries(List<RunMetadata> metadata) {
		Map<String, EvaluationAccumulator> accumulators = new LinkedHashMap<>();
		for (RunMetadata run : metadata) {
			Path evaluationPath = pathOrNull(run.artifactPaths().evaluationJson());
			if (evaluationPath == null || !Files.exists(evaluationPath)) {
				continue;
			}
			EvaluationReport report = read(evaluationPath, EvaluationReport.class);
			for (EvaluationDimensionResult dimension : safeList(report.dimensions())) {
				accumulators.computeIfAbsent(dimension.name(), EvaluationAccumulator::new).add(dimension);
			}
		}
		Map<String, ReportEvaluationSummary> summaries = new LinkedHashMap<>();
		for (EvaluationAccumulator accumulator : accumulators.values()) {
			summaries.put(accumulator.dimension, accumulator.summary());
		}
		return summaries;
	}

	private Map<String, Long> severityCounts(List<RunMetadata> metadata) {
		Map<String, Long> counts = new LinkedHashMap<>();
		for (RunMetadata run : metadata) {
			Path analysisPath = pathOrNull(run.artifactPaths().analysisJson());
			if (analysisPath == null || !Files.exists(analysisPath)) {
				continue;
			}
			AnalysisReport report = read(analysisPath, AnalysisReport.class);
			for (AnalysisFinding finding : safeList(report.findings())) {
				String severity = finding.severity() == null || finding.severity().isBlank()
						? "unknown"
						: finding.severity();
				counts.put(severity, counts.getOrDefault(severity, 0L) + 1);
			}
		}
		return counts;
	}

	private List<ReportScenarioCoverageSummary> coverageSummaries() {
		if (!Files.isDirectory(scenarioDirectory)) {
			return List.of();
		}
		try {
			return Files.list(scenarioDirectory)
					.filter(path -> path.getFileName().toString().endsWith(".yaml"))
					.sorted()
					.map(scenarioLoader::load)
					.map(this::coverageSummary)
					.toList();
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to read scenario coverage metadata", exception);
		}
	}

	private ReportScenarioCoverageSummary coverageSummary(Scenario scenario) {
		Scenario.Coverage coverage = scenario.coverage();
		if (coverage == null) {
			return new ReportScenarioCoverageSummary(scenario.id(), "", List.of(), "");
		}
		return new ReportScenarioCoverageSummary(
				scenario.id(),
				coverage.workflowArea(),
				coverage.edgeCases() == null ? List.of() : coverage.edgeCases(),
				coverage.riskFocus()
		);
	}

	private Path pathOrNull(String path) {
		if (path == null || path.isBlank()) {
			return null;
		}
		return Path.of(path);
	}

	private <T> T read(Path path, Class<T> type) {
		try {
			return objectMapper.readValue(path.toFile(), type);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to read report input: " + path, exception);
		}
	}

	private <T> List<T> safeList(List<T> values) {
		return values == null ? List.of() : values;
	}

	private static class EvaluationAccumulator {
		private final String dimension;
		private int scoredCount;
		private int insufficientEvidenceCount;
		private int scoreTotal;

		private EvaluationAccumulator(String dimension) {
			this.dimension = dimension;
		}

		private void add(EvaluationDimensionResult result) {
			if (result.insufficientEvidence()) {
				insufficientEvidenceCount++;
			}
			if (result.score() != null) {
				scoredCount++;
				scoreTotal += result.score();
			}
		}

		private ReportEvaluationSummary summary() {
			Double averageScore = scoredCount == 0 ? null : (double) scoreTotal / scoredCount;
			return new ReportEvaluationSummary(dimension, scoredCount, averageScore, insufficientEvidenceCount);
		}
	}
}
