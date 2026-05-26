package com.qaai.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.config.QaaiProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RunIndexWriter {

	private final ObjectMapper objectMapper;
	private final Path outputBaseDir;
	private final ArtifactCompletenessChecker completenessChecker;

	@Autowired
	public RunIndexWriter(ObjectMapper objectMapper, QaaiProperties properties,
			ArtifactCompletenessChecker completenessChecker) {
		this(objectMapper, Path.of(properties.outputs().baseDir()), completenessChecker);
	}

	public RunIndexWriter(ObjectMapper objectMapper, Path outputBaseDir,
			ArtifactCompletenessChecker completenessChecker) {
		this.objectMapper = objectMapper.findAndRegisterModules();
		this.outputBaseDir = outputBaseDir;
		this.completenessChecker = completenessChecker;
	}

	public RunIndexEntry append(RunMetadata metadata) {
		Path runDirectory = outputBaseDir.resolve(metadata.callId());
		ArtifactCompleteness completeness = completenessChecker.check(metadata);
		RunIndexEntry entry = new RunIndexEntry(
				metadata.callId(),
				metadata.scenarioId(),
				metadata.runMode(),
				metadata.status(),
				metadata.retellCallId(),
				metadata.startedAt(),
				metadata.endedAt(),
				runDirectory.toString(),
				metadata.artifactPaths().metadata(),
				completeness.complete(),
				completeness.missingRequiredArtifacts(),
				completeness.warnings(),
				metadata.artifactPaths()
		);

		try {
			Files.createDirectories(outputBaseDir);
			Files.writeString(indexPath(), objectMapper.writeValueAsString(entry) + System.lineSeparator(),
					java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
			return entry;
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to append run index entry for call_id: " + metadata.callId(),
					exception);
		}
	}

	public List<RunIndexEntry> readAll() {
		Path indexPath = indexPath();
		if (!Files.exists(indexPath)) {
			return List.of();
		}

		try {
			return Files.readAllLines(indexPath)
					.stream()
					.filter(line -> !line.isBlank())
					.map(this::readEntry)
					.toList();
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to read run index", exception);
		}
	}

	public Path indexPath() {
		return outputBaseDir.resolve("index.jsonl");
	}

	private RunIndexEntry readEntry(String line) {
		try {
			return objectMapper.readValue(line, RunIndexEntry.class);
		} catch (IOException exception) {
			throw new ArtifactWriteException("Unable to parse run index entry", exception);
		}
	}
}
