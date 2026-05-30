package com.qaai.evaluation;

import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.EvaluationMetadata;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.RuntimeReproducibilityMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EvaluationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationService.class);

	private final ArtifactWriter artifactWriter;
	private final ScenarioLoader scenarioLoader;
	private final EvaluationPromptBuilder promptBuilder;
	private final EvaluationClient evaluationClient;

	@Autowired
	public EvaluationService(
			ArtifactWriter artifactWriter,
			ScenarioLoader scenarioLoader,
			EvaluationPromptBuilder promptBuilder,
			EvaluationClient evaluationClient
	) {
		this.artifactWriter = artifactWriter;
		this.scenarioLoader = scenarioLoader;
		this.promptBuilder = promptBuilder;
		this.evaluationClient = evaluationClient;
	}

	public EvaluationResult evaluate(String callId) {
		if (callId == null || callId.isBlank()) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with --evaluate-call");
		}
		LOGGER.info("Starting evaluation call_id={}", callId);

		RunMetadata metadata = artifactWriter.readMetadata(callId);
		validateMetadata(callId, metadata);
		Path runDirectory = artifactWriter.runDirectory(callId);
		Scenario scenario = scenarioLoader.load(scenarioPath(metadata, runDirectory));
		NormalizedTranscript transcript = artifactWriter.readTranscript(callId);
		validateTranscript(callId, transcript);

		EvaluationReport report = evaluationClient.evaluate(new EvaluationRequest(
				scenario,
				transcript,
				promptBuilder.build(scenario, transcript)
		));
		EvaluationReport validatedReport = validateReport(metadata, transcript, report);
		RunMetadata updatedMetadata = updateMetadata(metadata, runDirectory);
		String markdown = buildMarkdown(validatedReport);
		artifactWriter.writeEvaluationArtifacts(callId, updatedMetadata, validatedReport, markdown);

		LOGGER.info("Completed evaluation call_id={} evaluation_json={} evaluation_markdown={}", callId,
				runDirectory.resolve("evaluation.json"), runDirectory.resolve("evaluation.md"));
		return new EvaluationResult(
				updatedMetadata,
				runDirectory,
				runDirectory.resolve("evaluation.json"),
				runDirectory.resolve("evaluation.md")
		);
	}

	private void validateMetadata(String callId, RunMetadata metadata) {
		if (!callId.equals(metadata.callId())) {
			throw new IllegalArgumentException("metadata.json call_id does not match requested call_id: " + callId);
		}
		if (metadata.artifactPaths() == null || isBlank(metadata.artifactPaths().transcriptJson())) {
			throw new IllegalArgumentException("Evaluation requires metadata.artifact_paths.transcript_json");
		}
		if (!Files.exists(Path.of(metadata.artifactPaths().transcriptJson()))) {
			throw new IllegalArgumentException("Evaluation requires an existing transcript.json artifact");
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
			throw new IllegalArgumentException("Evaluation requires at least one transcript turn");
		}
	}

	private EvaluationReport validateReport(
			RunMetadata metadata,
			NormalizedTranscript transcript,
			EvaluationReport report
	) {
		if (report == null) {
			throw new EvaluationException("Evaluation client returned no report");
		}
		if (!metadata.callId().equals(report.callId())) {
			throw new EvaluationException("Evaluation report call_id does not match metadata");
		}
		if (!metadata.scenarioId().equals(report.scenarioId())) {
			throw new EvaluationException("Evaluation report scenario_id does not match metadata");
		}
		if (!report.humanReviewRequired()) {
			throw new EvaluationException("Evaluation report must require human review");
		}

		Set<String> supportedDimensions = Set.copyOf(EvaluationDimension.supportedValues());
		for (EvaluationDimensionResult dimension : nullToEmpty(report.dimensions())) {
			validateDimension(transcript, supportedDimensions, dimension);
		}
		if (nullToEmpty(report.dimensions()).isEmpty()) {
			throw new EvaluationException("Evaluation report must include at least one dimension");
		}
		return report;
	}

	private void validateDimension(
			NormalizedTranscript transcript,
			Set<String> supportedDimensions,
			EvaluationDimensionResult dimension
	) {
		if (dimension == null || isBlank(dimension.name()) || !supportedDimensions.contains(dimension.name())) {
			throw new EvaluationException("Evaluation report includes unsupported dimension");
		}
		if (dimension.insufficientEvidence()) {
			if (dimension.score() != null) {
				throw new EvaluationException("Insufficient-evidence dimension must not include a score: "
						+ dimension.name());
			}
			return;
		}
		if (dimension.score() == null || dimension.score() < 1 || dimension.score() > 5) {
			throw new EvaluationException("Evaluation dimension score must be between 1 and 5: " + dimension.name());
		}
		if (nullToEmpty(dimension.evidence()).isEmpty()) {
			throw new EvaluationException("Evaluation dimension is missing transcript evidence: " + dimension.name());
		}
		for (EvaluationEvidenceReference evidence : dimension.evidence()) {
			validateEvidence(transcript, evidence, dimension.name());
		}
	}

	private void validateEvidence(
			NormalizedTranscript transcript,
			EvaluationEvidenceReference evidence,
			String dimensionName
	) {
		if (evidence == null || isBlank(evidence.quote())) {
			throw new EvaluationException("Evaluation dimension has blank evidence quote: " + dimensionName);
		}
		boolean matchesTranscript = transcript.turns().stream().anyMatch(turn -> turnMatchesEvidence(turn, evidence));
		if (!matchesTranscript) {
			throw new EvaluationException("Evidence quote was not found in transcript for dimension: " + dimensionName);
		}
	}

	private boolean turnMatchesEvidence(TranscriptTurn turn, EvaluationEvidenceReference evidence) {
		if (turn.text() == null || !turn.text().contains(evidence.quote())) {
			return false;
		}
		if (!isBlank(evidence.speaker()) && !evidence.speaker().equals(turn.speaker())) {
			return false;
		}
		return evidence.turnIndex() == null || evidence.turnIndex().equals(turn.index());
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
				existingPaths.analysisJson(),
				existingPaths.analysisMarkdown(),
				runDirectory.resolve("evaluation.json").toString(),
				runDirectory.resolve("evaluation.md").toString(),
				existingPaths.multiLensReviewJson(),
				existingPaths.multiLensReviewMarkdown(),
				existingPaths.observationsMarkdown()
		);
		return new RunMetadata(
				metadata.callId(),
				metadata.scenarioId(),
				metadata.runMode(),
				metadata.channel(),
				metadata.targetPhoneNumber(),
				metadata.retellCallId(),
				metadata.startedAt(),
				metadata.endedAt(),
				metadata.callDurationSeconds(),
				"evaluation_completed",
				artifactPaths,
				metadata.analysis(),
				new EvaluationMetadata(evaluationClient.provider(), evaluationClient.model()),
				RuntimeReproducibilityMetadata.forCommand("evaluate-call")
		);
	}

	private String buildMarkdown(EvaluationReport report) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# Evidence-Linked Evaluation").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("call_id: ").append(report.callId()).append(System.lineSeparator());
		markdown.append("scenario_id: ").append(report.scenarioId()).append(System.lineSeparator());
		markdown.append("human_review_required: true").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("## Dimensions").append(System.lineSeparator());
		for (EvaluationDimensionResult dimension : nullToEmpty(report.dimensions())) {
			markdown.append("- ").append(dimension.name()).append(System.lineSeparator());
			markdown.append("  - Score: ")
					.append(dimension.score() == null ? "insufficient evidence" : dimension.score() + " / 5")
					.append(System.lineSeparator());
			markdown.append("  - Uncertainty: ").append(dimension.uncertainty()).append(System.lineSeparator());
			markdown.append("  - Rationale: ").append(dimension.rationale()).append(System.lineSeparator());
			for (EvaluationEvidenceReference evidence : nullToEmpty(dimension.evidence())) {
				markdown.append("  - Evidence turn ")
						.append(evidence.turnIndex())
						.append(" [")
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
		markdown.append("- Evaluation scores are advisory. A human reviewer owns final judgment.")
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
