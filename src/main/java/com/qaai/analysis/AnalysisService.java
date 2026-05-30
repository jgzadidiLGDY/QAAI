package com.qaai.analysis;

import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.AnalysisMetadata;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.RuntimeReproducibilityMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnalysisService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisService.class);

	private final ArtifactWriter artifactWriter;
	private final ScenarioLoader scenarioLoader;
	private final AnalysisPromptBuilder promptBuilder;
	private final AnalysisClient analysisClient;

	@Autowired
	public AnalysisService(
			ArtifactWriter artifactWriter,
			ScenarioLoader scenarioLoader,
			AnalysisPromptBuilder promptBuilder,
			AnalysisClient analysisClient
	) {
		this.artifactWriter = artifactWriter;
		this.scenarioLoader = scenarioLoader;
		this.promptBuilder = promptBuilder;
		this.analysisClient = analysisClient;
	}

	public AnalysisResult analyze(String callId) {
		if (callId == null || callId.isBlank()) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with --analyze-call");
		}
		LOGGER.info("Starting analysis call_id={}", callId);

		RunMetadata metadata = artifactWriter.readMetadata(callId);
		validateMetadata(callId, metadata);
		Path runDirectory = artifactWriter.runDirectory(callId);
		Scenario scenario = scenarioLoader.load(scenarioPath(metadata, runDirectory));
		NormalizedTranscript transcript = artifactWriter.readTranscript(callId);
		validateTranscript(callId, transcript);

		LOGGER.info("Requesting analysis call_id={} scenario_id={}", metadata.callId(), metadata.scenarioId());
		AnalysisReport report = analysisClient.analyze(new AnalysisRequest(
				scenario,
				transcript,
				promptBuilder.build(scenario, transcript)
		));
		AnalysisReport validatedReport = validateReport(metadata, transcript, report);
		RunMetadata updatedMetadata = updateMetadata(metadata, runDirectory);
		String markdown = buildMarkdown(validatedReport);
		artifactWriter.writeAnalysisArtifacts(callId, updatedMetadata, validatedReport, markdown);

		LOGGER.info("Completed analysis call_id={} analysis_json={} analysis_markdown={}", callId,
				runDirectory.resolve("analysis.json"), runDirectory.resolve("analysis.md"));
		return new AnalysisResult(
				updatedMetadata,
				runDirectory,
				runDirectory.resolve("analysis.json"),
				runDirectory.resolve("analysis.md")
		);
	}

	private void validateMetadata(String callId, RunMetadata metadata) {
		if (!callId.equals(metadata.callId())) {
			throw new IllegalArgumentException("metadata.json call_id does not match requested call_id: " + callId);
		}
		if (metadata.artifactPaths() == null || isBlank(metadata.artifactPaths().transcriptJson())) {
			throw new IllegalArgumentException("Analysis requires metadata.artifact_paths.transcript_json");
		}
		if (!Files.exists(Path.of(metadata.artifactPaths().transcriptJson()))) {
			throw new IllegalArgumentException("Analysis requires an existing transcript.json artifact");
		}
	}

	private Path scenarioPath(RunMetadata metadata, Path runDirectory) {
		if (metadata.artifactPaths() != null && !isBlank(metadata.artifactPaths().scenario())) {
			return Path.of(metadata.artifactPaths().scenario());
		}
		return runDirectory.resolve("scenario.yaml");
	}

	private void validateTranscript(String callId, NormalizedTranscript transcript) {
		if (!callId.equals(transcript.callId())) {
			throw new IllegalArgumentException("transcript.json call_id does not match requested call_id: " + callId);
		}
		if (transcript.turns() == null || transcript.turns().isEmpty()) {
			throw new IllegalArgumentException("Analysis requires at least one transcript turn");
		}
	}

	private AnalysisReport validateReport(
			RunMetadata metadata,
			NormalizedTranscript transcript,
			AnalysisReport report
	) {
		if (report == null) {
			throw new AnalysisException("Analysis client returned no report");
		}
		if (!metadata.callId().equals(report.callId())) {
			throw new AnalysisException("Analysis report call_id does not match metadata");
		}
		if (!metadata.scenarioId().equals(report.scenarioId())) {
			throw new AnalysisException("Analysis report scenario_id does not match metadata");
		}
		if (!report.humanReviewRequired()) {
			throw new AnalysisException("Analysis report must require human review");
		}

		for (AnalysisFinding finding : nullToEmpty(report.findings())) {
			if (nullToEmpty(finding.evidence()).isEmpty()) {
				throw new AnalysisException("Analysis finding is missing transcript evidence: " + finding.title());
			}
			for (EvidenceReference evidence : finding.evidence()) {
				validateEvidence(transcript, evidence, finding.title());
			}
		}
		return report;
	}

	private void validateEvidence(NormalizedTranscript transcript, EvidenceReference evidence, String title) {
		if (evidence == null || isBlank(evidence.quote())) {
			throw new AnalysisException("Analysis finding has blank evidence quote: " + title);
		}
		boolean matchesTranscript = transcript.turns().stream().anyMatch(turn -> turnMatchesEvidence(turn, evidence));
		if (!matchesTranscript) {
			throw new AnalysisException("Evidence quote was not found in transcript for finding: " + title);
		}
	}

	private boolean turnMatchesEvidence(TranscriptTurn turn, EvidenceReference evidence) {
		if (turn.text() == null || !turn.text().contains(evidence.quote())) {
			return false;
		}
		return isBlank(evidence.speaker()) || evidence.speaker().equals(turn.speaker());
	}

	private RunMetadata updateMetadata(RunMetadata metadata, Path runDirectory) {
		ArtifactPaths existingPaths = metadata.artifactPaths();
		ArtifactPaths artifactPaths = new ArtifactPaths(
				existingPaths.scenario(),
				runDirectory.resolve("metadata.json").toString(),
				existingPaths.transcriptText(),
				existingPaths.transcriptJson(),
				existingPaths.patientSimulation(),
				existingPaths.audio(),
				existingPaths.manifest(),
				runDirectory.resolve("analysis.json").toString(),
				runDirectory.resolve("analysis.md").toString(),
				existingPaths.evaluationJson(),
				existingPaths.evaluationMarkdown(),
				existingPaths.multiLensReviewJson(),
				existingPaths.multiLensReviewMarkdown(),
				existingPaths.observationsMarkdown()
		);
		return new RunMetadata(
				metadata.callId(),
				metadata.scenarioId(),
				metadata.agentProfileId(),
				metadata.suiteId(),
				metadata.suiteRunId(),
				metadata.runMode(),
				metadata.channel(),
				metadata.targetPhoneNumber(),
				metadata.retellCallId(),
				metadata.startedAt(),
				metadata.endedAt(),
				metadata.callDurationSeconds(),
				"analysis_completed",
				artifactPaths,
				new AnalysisMetadata(analysisClient.provider(), analysisClient.model()),
				metadata.evaluation(),
				RuntimeReproducibilityMetadata.forCommand("analyze-call")
		);
	}

	private String buildMarkdown(AnalysisReport report) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# AI-Assisted Analysis").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("call_id: ").append(report.callId()).append(System.lineSeparator());
		markdown.append("scenario_id: ").append(report.scenarioId()).append(System.lineSeparator());
		markdown.append("human_review_required: true").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("## Summary").append(System.lineSeparator());
		markdown.append(report.summary()).append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("## Findings").append(System.lineSeparator());
		if (nullToEmpty(report.findings()).isEmpty()) {
			markdown.append("- No evidence-supported findings were suggested.").append(System.lineSeparator());
		}
		for (AnalysisFinding finding : nullToEmpty(report.findings())) {
			markdown.append("- ")
					.append(finding.title())
					.append(" (")
					.append(finding.severity())
					.append(")")
					.append(System.lineSeparator());
			markdown.append("  - Expected: ").append(finding.expectedBehavior()).append(System.lineSeparator());
			markdown.append("  - Actual: ").append(finding.actualBehavior()).append(System.lineSeparator());
			for (EvidenceReference evidence : nullToEmpty(finding.evidence())) {
				markdown.append("  - Evidence [")
						.append(evidence.speaker())
						.append("]: ")
						.append(evidence.quote())
						.append(System.lineSeparator());
			}
		}
		markdown.append(System.lineSeparator());
		markdown.append("## Notes").append(System.lineSeparator());
		for (String note : nullToEmpty(report.notes())) {
			markdown.append("- ").append(note).append(System.lineSeparator());
		}
		markdown.append("- AI findings are advisory. A human reviewer owns final judgment.")
				.append(System.lineSeparator());
		return markdown.toString();
	}

	private <T> List<T> nullToEmpty(List<T> values) {
		return values == null ? List.of() : values;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
