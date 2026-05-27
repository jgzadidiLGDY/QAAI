package com.qaai.runner;

import com.qaai.artifacts.ArtifactCompleteness;
import com.qaai.artifacts.ArtifactCompletenessChecker;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.RunIndexEntry;
import com.qaai.artifacts.RunIndexWriter;
import com.qaai.artifacts.RunMetadata;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RunInspectionService {

	private final ArtifactWriter artifactWriter;
	private final ArtifactCompletenessChecker completenessChecker;
	private final RunIndexWriter runIndexWriter;

	@Autowired
	public RunInspectionService(
			ArtifactWriter artifactWriter,
			ArtifactCompletenessChecker completenessChecker,
			RunIndexWriter runIndexWriter
	) {
		this.artifactWriter = artifactWriter;
		this.completenessChecker = completenessChecker;
		this.runIndexWriter = runIndexWriter;
	}

	public RunInspection showRun(String callId) {
		if (callId == null || callId.isBlank()) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with --show-run");
		}

		RunMetadata metadata = artifactWriter.readMetadata(callId);
		if (!callId.equals(metadata.callId())) {
			throw new IllegalArgumentException("metadata.json call_id does not match requested call_id: " + callId);
		}

		Path runDirectory = artifactWriter.runDirectory(callId);
		ArtifactCompleteness completeness = completenessChecker.check(metadata);
		RunIndexEntry latestIndexEntry = latestIndexEntry(callId);
		return new RunInspection(metadata, runDirectory, completeness, latestIndexEntry);
	}

	public List<RunIndexEntry> listRuns(RunFilters filters) {
		RunFilters safeFilters = filters == null ? new RunFilters(null, null, null) : filters;
		return runIndexWriter.readAll()
				.stream()
				.filter(entry -> matches(safeFilters.scenarioId(), entry.scenarioId()))
				.filter(entry -> matches(safeFilters.status(), entry.status()))
				.filter(entry -> matches(normalizeRunMode(safeFilters.runMode()), entry.runMode()))
				.toList();
	}

	private RunIndexEntry latestIndexEntry(String callId) {
		List<RunIndexEntry> matches = runIndexWriter.readAll()
				.stream()
				.filter(entry -> callId.equals(entry.callId()))
				.toList();
		if (matches.isEmpty()) {
			return null;
		}
		return matches.getLast();
	}

	private boolean matches(String expected, String actual) {
		return expected == null || expected.isBlank() || expected.equals(actual);
	}

	private String normalizeRunMode(String runMode) {
		if ("dry-run".equals(runMode)) {
			return "dry_run";
		}
		return runMode;
	}
}
