package com.qaai.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.analysis.AnalysisReport;
import com.qaai.config.QaaiProperties;
import com.qaai.evaluation.EvaluationReport;
import com.qaai.review.MultiLensReviewReport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArtifactWriter {

	private static final String CALL_ID_PATTERN = "[A-Za-z0-9._-]+";

	private final ObjectMapper objectMapper;
	private final Path outputBaseDir;
	private final RunIndexWriter runIndexWriter;

	@Autowired
	public ArtifactWriter(ObjectMapper objectMapper, QaaiProperties properties) {
		this(objectMapper, Path.of(properties.outputs().baseDir()));
	}

	public ArtifactWriter(ObjectMapper objectMapper, Path outputBaseDir) {
		this.objectMapper = objectMapper.findAndRegisterModules();
		this.outputBaseDir = outputBaseDir;
		this.runIndexWriter = new RunIndexWriter(
				this.objectMapper,
				outputBaseDir,
				new ArtifactCompletenessChecker()
		);
	}

	public ArtifactWriter(ObjectMapper objectMapper, Path outputBaseDir, RunIndexWriter runIndexWriter) {
		this.objectMapper = objectMapper.findAndRegisterModules();
		this.outputBaseDir = outputBaseDir;
		this.runIndexWriter = runIndexWriter;
	}

	public ArtifactBundle writeDryRunArtifacts(
			String callId,
			Path scenarioPath,
			RunMetadata metadata,
			String patientSimulationPrompt,
			String transcriptText,
			String observationsMarkdown
	) {
		Path runDirectory = runDirectory(callId);
		Path scenarioSnapshot = runDirectory.resolve("scenario.yaml");
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path patientSimulationPath = runDirectory.resolve("patient_simulation.md");
		Path transcriptPath = runDirectory.resolve("transcript.txt");
		Path observationsPath = runDirectory.resolve("observations.md");

		try {
			Files.createDirectories(runDirectory);
			Files.copy(scenarioPath, scenarioSnapshot, StandardCopyOption.REPLACE_EXISTING);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			Files.writeString(patientSimulationPath, patientSimulationPrompt);
			Files.writeString(transcriptPath, transcriptText);
			Files.writeString(observationsPath, observationsMarkdown);
			runIndexWriter.append(metadata);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write dry-run artifacts for call_id: " + callId, exception);
		}

		return new ArtifactBundle(
				callId,
				runDirectory,
				scenarioSnapshot,
				metadataPath,
				transcriptPath,
				patientSimulationPath,
				observationsPath
		);
	}

	public ArtifactBundle writeCallStartedArtifacts(
			String callId,
			Path scenarioPath,
			RunMetadata metadata,
			String patientSimulationPrompt,
			String observationsMarkdown
	) {
		Path runDirectory = runDirectory(callId);
		Path scenarioSnapshot = runDirectory.resolve("scenario.yaml");
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path patientSimulationPath = runDirectory.resolve("patient_simulation.md");
		Path observationsPath = runDirectory.resolve("observations.md");

		try {
			Files.createDirectories(runDirectory);
			Files.copy(scenarioPath, scenarioSnapshot, StandardCopyOption.REPLACE_EXISTING);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			Files.writeString(patientSimulationPath, patientSimulationPrompt);
			Files.writeString(observationsPath, observationsMarkdown);
			runIndexWriter.append(metadata);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write Retell call artifacts for call_id: " + callId, exception);
		}

		return new ArtifactBundle(
				callId,
				runDirectory,
				scenarioSnapshot,
				metadataPath,
				null,
				patientSimulationPath,
				observationsPath
		);
	}

	public void writeCapturedArtifacts(
			String callId,
			RunMetadata metadata,
			NormalizedTranscript transcript,
			String transcriptText,
			byte[] audioBytes,
			ArtifactManifest manifest
	) {
		Path runDirectory = runDirectory(callId);
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path transcriptJsonPath = runDirectory.resolve("transcript.json");
		Path transcriptTextPath = runDirectory.resolve("transcript.txt");
		Path manifestPath = runDirectory.resolve("manifest.json");
		Path audioPath = runDirectory.resolve("audio.wav");

		try {
			Files.createDirectories(runDirectory);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(transcriptJsonPath.toFile(), transcript);
			Files.writeString(transcriptTextPath, transcriptText);
			if (audioBytes != null && audioBytes.length > 0) {
				Files.write(audioPath, audioBytes);
			}
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
			runIndexWriter.append(metadata);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write captured artifacts for call_id: " + callId, exception);
		}
	}

	public void writeAnalysisArtifacts(
			String callId,
			RunMetadata metadata,
			AnalysisReport analysisReport,
			String analysisMarkdown
	) {
		Path runDirectory = runDirectory(callId);
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path analysisJsonPath = runDirectory.resolve("analysis.json");
		Path analysisMarkdownPath = runDirectory.resolve("analysis.md");

		try {
			Files.createDirectories(runDirectory);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(analysisJsonPath.toFile(), analysisReport);
			Files.writeString(analysisMarkdownPath, analysisMarkdown);
			updateManifestWithAnalysis(runDirectory, metadata);
			runIndexWriter.append(metadata);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write analysis artifacts for call_id: " + callId, exception);
		}
	}

	public void writeEvaluationArtifacts(
			String callId,
			RunMetadata metadata,
			EvaluationReport evaluationReport,
			String evaluationMarkdown
	) {
		Path runDirectory = runDirectory(callId);
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path evaluationJsonPath = runDirectory.resolve("evaluation.json");
		Path evaluationMarkdownPath = runDirectory.resolve("evaluation.md");

		try {
			Files.createDirectories(runDirectory);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(evaluationJsonPath.toFile(), evaluationReport);
			Files.writeString(evaluationMarkdownPath, evaluationMarkdown);
			updateManifestWithEvaluation(runDirectory, metadata);
			runIndexWriter.append(metadata);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write evaluation artifacts for call_id: " + callId, exception);
		}
	}

	public void writeMultiLensReviewArtifacts(
			String callId,
			RunMetadata metadata,
			MultiLensReviewReport reviewReport,
			String reviewMarkdown
	) {
		Path runDirectory = runDirectory(callId);
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path reviewJsonPath = runDirectory.resolve("multi-lens-review.json");
		Path reviewMarkdownPath = runDirectory.resolve("multi-lens-review.md");

		try {
			Files.createDirectories(runDirectory);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(reviewJsonPath.toFile(), reviewReport);
			Files.writeString(reviewMarkdownPath, reviewMarkdown);
			updateManifestWithMultiLensReview(runDirectory, metadata);
			runIndexWriter.append(metadata);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write multi-lens review artifacts for call_id: " + callId,
					exception);
		}
	}

	public Path writeConversationQualityReview(
			String callId,
			RunMetadata metadata,
			String observationsMarkdown
	) {
		Path runDirectory = runDirectory(callId);
		Path observationsPath = runDirectory.resolve("observations.md");

		try {
			Files.createDirectories(runDirectory);
			Files.writeString(observationsPath, observationsMarkdown);
			runIndexWriter.append(metadata);
		} catch (IOException exception) {
			throw new ArtifactWriteException(
					"Unable to write conversation-quality observations for call_id: " + callId,
					exception
			);
		}

		return observationsPath;
	}

	public List<RunIndexEntry> readRunIndex() {
		return runIndexWriter.readAll();
	}

	public RunMetadata readMetadata(String callId) {
		Path metadataPath = runDirectory(callId).resolve("metadata.json");

		try {
			if (!Files.exists(metadataPath)) {
				throw new ArtifactWriteException("No metadata.json found for call_id: " + callId);
			}
			return objectMapper.readValue(metadataPath.toFile(), RunMetadata.class);
		} catch (ArtifactWriteException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to read metadata for call_id: " + callId, exception);
		}
	}

	public NormalizedTranscript readTranscript(String callId) {
		Path transcriptPath = runDirectory(callId).resolve("transcript.json");

		try {
			if (!Files.exists(transcriptPath)) {
				throw new ArtifactWriteException("No transcript.json found for call_id: " + callId);
			}
			return objectMapper.readValue(transcriptPath.toFile(), NormalizedTranscript.class);
		} catch (ArtifactWriteException exception) {
			throw exception;
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to read transcript for call_id: " + callId, exception);
		}
	}

	public Path runDirectory(String callId) {
		validateCallId(callId);
		return outputBaseDir.resolve(callId);
	}

	private void validateCallId(String callId) {
		if (callId == null || callId.isBlank()) {
			throw new IllegalArgumentException("call_id is required");
		}
		if (!callId.matches(CALL_ID_PATTERN)) {
			throw new IllegalArgumentException(
					"call_id may contain only letters, numbers, dots, underscores, and hyphens"
			);
		}
	}

	private void updateManifestWithAnalysis(Path runDirectory, RunMetadata metadata) throws IOException {
		if (metadata.artifactPaths().manifest() == null || !Files.exists(Path.of(metadata.artifactPaths().manifest()))) {
			return;
		}

		Path manifestPath = Path.of(metadata.artifactPaths().manifest());
		ArtifactManifest existingManifest = objectMapper.readValue(manifestPath.toFile(), ArtifactManifest.class);
		List<ArtifactManifest.ArtifactEntry> entries = new ArrayList<>(existingManifest.artifacts());
		entries.removeIf(entry -> "analysis_json".equals(entry.name()) || "analysis_markdown".equals(entry.name()));
		entries.add(new ArtifactManifest.ArtifactEntry(
				"analysis_json",
				runDirectory.resolve("analysis.json").toString(),
				true,
				"Generated during Phase 6 AI-assisted analysis."
		));
		entries.add(new ArtifactManifest.ArtifactEntry(
				"analysis_markdown",
				runDirectory.resolve("analysis.md").toString(),
				true,
				"Generated during Phase 6 AI-assisted analysis."
		));
		ArtifactManifest updatedManifest = new ArtifactManifest(
				existingManifest.callId(),
				existingManifest.scenarioId(),
				existingManifest.retellCallId(),
				existingManifest.capturedAt(),
				existingManifest.retellStatus(),
				existingManifest.recordingUrl(),
				entries
		);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), updatedManifest);
	}

	private void updateManifestWithEvaluation(Path runDirectory, RunMetadata metadata) throws IOException {
		if (metadata.artifactPaths().manifest() == null || !Files.exists(Path.of(metadata.artifactPaths().manifest()))) {
			return;
		}

		Path manifestPath = Path.of(metadata.artifactPaths().manifest());
		ArtifactManifest existingManifest = objectMapper.readValue(manifestPath.toFile(), ArtifactManifest.class);
		List<ArtifactManifest.ArtifactEntry> entries = new ArrayList<>(existingManifest.artifacts());
		entries.removeIf(entry -> "evaluation_json".equals(entry.name()) || "evaluation_markdown".equals(entry.name()));
		entries.add(new ArtifactManifest.ArtifactEntry(
				"evaluation_json",
				runDirectory.resolve("evaluation.json").toString(),
				true,
				"Generated during Phase 15 evidence-linked evaluation."
		));
		entries.add(new ArtifactManifest.ArtifactEntry(
				"evaluation_markdown",
				runDirectory.resolve("evaluation.md").toString(),
				true,
				"Generated during Phase 15 evidence-linked evaluation."
		));
		ArtifactManifest updatedManifest = new ArtifactManifest(
				existingManifest.callId(),
				existingManifest.scenarioId(),
				existingManifest.retellCallId(),
				existingManifest.capturedAt(),
				existingManifest.retellStatus(),
				existingManifest.recordingUrl(),
				entries
		);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), updatedManifest);
	}

	private void updateManifestWithMultiLensReview(Path runDirectory, RunMetadata metadata) throws IOException {
		if (metadata.artifactPaths().manifest() == null || !Files.exists(Path.of(metadata.artifactPaths().manifest()))) {
			return;
		}

		Path manifestPath = Path.of(metadata.artifactPaths().manifest());
		ArtifactManifest existingManifest = objectMapper.readValue(manifestPath.toFile(), ArtifactManifest.class);
		List<ArtifactManifest.ArtifactEntry> entries = new ArrayList<>(existingManifest.artifacts());
		entries.removeIf(entry -> "multi_lens_review_json".equals(entry.name())
				|| "multi_lens_review_markdown".equals(entry.name()));
		entries.add(new ArtifactManifest.ArtifactEntry(
				"multi_lens_review_json",
				runDirectory.resolve("multi-lens-review.json").toString(),
				true,
				"Generated during Phase 18 structured multi-lens review."
		));
		entries.add(new ArtifactManifest.ArtifactEntry(
				"multi_lens_review_markdown",
				runDirectory.resolve("multi-lens-review.md").toString(),
				true,
				"Generated during Phase 18 structured multi-lens review."
		));
		ArtifactManifest updatedManifest = new ArtifactManifest(
				existingManifest.callId(),
				existingManifest.scenarioId(),
				existingManifest.retellCallId(),
				existingManifest.capturedAt(),
				existingManifest.retellStatus(),
				existingManifest.recordingUrl(),
				entries
		);
		objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), updatedManifest);
	}
}
