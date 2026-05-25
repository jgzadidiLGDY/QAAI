package com.qaai.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.config.QaaiProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArtifactWriter {

	private final ObjectMapper objectMapper;
	private final Path outputBaseDir;

	@Autowired
	public ArtifactWriter(ObjectMapper objectMapper, QaaiProperties properties) {
		this(objectMapper, Path.of(properties.outputs().baseDir()));
	}

	public ArtifactWriter(ObjectMapper objectMapper, Path outputBaseDir) {
		this.objectMapper = objectMapper.findAndRegisterModules();
		this.outputBaseDir = outputBaseDir;
	}

	public ArtifactBundle writeDryRunArtifacts(
			String callId,
			Path scenarioPath,
			RunMetadata metadata,
			String transcriptText,
			String observationsMarkdown
	) {
		Path runDirectory = outputBaseDir.resolve(callId);
		Path scenarioSnapshot = runDirectory.resolve("scenario.yaml");
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path transcriptPath = runDirectory.resolve("transcript.txt");
		Path observationsPath = runDirectory.resolve("observations.md");

		try {
			Files.createDirectories(runDirectory);
			Files.copy(scenarioPath, scenarioSnapshot, StandardCopyOption.REPLACE_EXISTING);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			Files.writeString(transcriptPath, transcriptText);
			Files.writeString(observationsPath, observationsMarkdown);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write dry-run artifacts for call_id: " + callId, exception);
		}

		return new ArtifactBundle(callId, runDirectory, scenarioSnapshot, metadataPath, transcriptPath, observationsPath);
	}

	public ArtifactBundle writeCallStartedArtifacts(
			String callId,
			Path scenarioPath,
			RunMetadata metadata,
			String observationsMarkdown
	) {
		Path runDirectory = outputBaseDir.resolve(callId);
		Path scenarioSnapshot = runDirectory.resolve("scenario.yaml");
		Path metadataPath = runDirectory.resolve("metadata.json");
		Path observationsPath = runDirectory.resolve("observations.md");

		try {
			Files.createDirectories(runDirectory);
			Files.copy(scenarioPath, scenarioSnapshot, StandardCopyOption.REPLACE_EXISTING);
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
			Files.writeString(observationsPath, observationsMarkdown);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write Retell call artifacts for call_id: " + callId, exception);
		}

		return new ArtifactBundle(callId, runDirectory, scenarioSnapshot, metadataPath, null, observationsPath);
	}

	public void writeCapturedArtifacts(
			String callId,
			RunMetadata metadata,
			NormalizedTranscript transcript,
			String transcriptText,
			byte[] audioBytes,
			ArtifactManifest manifest
	) {
		Path runDirectory = outputBaseDir.resolve(callId);
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
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to write captured artifacts for call_id: " + callId, exception);
		}
	}

	public RunMetadata readMetadata(String callId) {
		Path metadataPath = outputBaseDir.resolve(callId).resolve("metadata.json");

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

	public Path runDirectory(String callId) {
		return outputBaseDir.resolve(callId);
	}
}
