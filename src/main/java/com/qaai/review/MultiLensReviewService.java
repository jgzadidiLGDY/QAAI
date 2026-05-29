package com.qaai.review;

import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.RuntimeReproducibilityMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.scenario.Scenario;
import com.qaai.scenario.ScenarioLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MultiLensReviewService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultiLensReviewService.class);

	private final ArtifactWriter artifactWriter;
	private final ScenarioLoader scenarioLoader;
	private final MultiLensReviewClient reviewClient;
	private final Clock clock;

	@Autowired
	public MultiLensReviewService(
			ArtifactWriter artifactWriter,
			ScenarioLoader scenarioLoader,
			MultiLensReviewClient reviewClient
	) {
		this(artifactWriter, scenarioLoader, reviewClient, Clock.systemDefaultZone());
	}

	public MultiLensReviewService(
			ArtifactWriter artifactWriter,
			ScenarioLoader scenarioLoader,
			MultiLensReviewClient reviewClient,
			Clock clock
	) {
		this.artifactWriter = artifactWriter;
		this.scenarioLoader = scenarioLoader;
		this.reviewClient = reviewClient;
		this.clock = clock;
	}

	public MultiLensReviewResult review(String callId) {
		if (callId == null || callId.isBlank()) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with --multi-lens-review");
		}
		LOGGER.info("Starting multi-lens review call_id={}", callId);

		RunMetadata metadata = artifactWriter.readMetadata(callId);
		validateMetadata(callId, metadata);
		Path runDirectory = artifactWriter.runDirectory(callId);
		Scenario scenario = scenarioLoader.load(scenarioPath(metadata, runDirectory));
		NormalizedTranscript transcript = artifactWriter.readTranscript(callId);
		validateTranscript(callId, transcript);

		OffsetDateTime generatedAt = OffsetDateTime.now(clock);
		String reviewId = reviewId(generatedAt, callId);
		MultiLensReviewReport report = reviewClient.review(new MultiLensReviewRequest(
				reviewId,
				generatedAt,
				scenario,
				metadata,
				transcript,
				optionalPath(metadata.artifactPaths().analysisJson()),
				optionalPath(metadata.artifactPaths().evaluationJson())
		));
		MultiLensReviewReport validatedReport = validateReport(metadata, transcript, report);
		RunMetadata updatedMetadata = updateMetadata(metadata, runDirectory);
		String markdown = buildMarkdown(validatedReport);
		artifactWriter.writeMultiLensReviewArtifacts(callId, updatedMetadata, validatedReport, markdown);

		LOGGER.info("Completed multi-lens review call_id={} review_json={} review_markdown={}", callId,
				runDirectory.resolve("multi-lens-review.json"), runDirectory.resolve("multi-lens-review.md"));
		return new MultiLensReviewResult(
				updatedMetadata,
				runDirectory,
				runDirectory.resolve("multi-lens-review.json"),
				runDirectory.resolve("multi-lens-review.md")
		);
	}

	private void validateMetadata(String callId, RunMetadata metadata) {
		if (!callId.equals(metadata.callId())) {
			throw new IllegalArgumentException("metadata.json call_id does not match requested call_id: " + callId);
		}
		if (metadata.artifactPaths() == null || isBlank(metadata.artifactPaths().transcriptJson())) {
			throw new IllegalArgumentException("Multi-lens review requires metadata.artifact_paths.transcript_json");
		}
		if (!Files.exists(Path.of(metadata.artifactPaths().transcriptJson()))) {
			throw new IllegalArgumentException("Multi-lens review requires an existing transcript.json artifact");
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
			throw new IllegalArgumentException("Multi-lens review requires at least one transcript turn");
		}
	}

	private MultiLensReviewReport validateReport(
			RunMetadata metadata,
			NormalizedTranscript transcript,
			MultiLensReviewReport report
	) {
		if (report == null) {
			throw new MultiLensReviewException("Multi-lens review client returned no report");
		}
		if (!metadata.callId().equals(report.callId())) {
			throw new MultiLensReviewException("Review report call_id does not match metadata");
		}
		if (!metadata.scenarioId().equals(report.scenarioId())) {
			throw new MultiLensReviewException("Review report scenario_id does not match metadata");
		}
		if (!report.humanReviewRequired()) {
			throw new MultiLensReviewException("Review report must require human review");
		}
		if (nullToEmpty(report.lenses()).isEmpty()) {
			throw new MultiLensReviewException("Review report must include at least one lens");
		}

		Set<String> supportedLensIds = Set.copyOf(ReviewLens.supportedIds());
		Set<String> seenLensIds = new HashSet<>();
		for (ReviewLensResult lens : nullToEmpty(report.lenses())) {
			validateLens(transcript, supportedLensIds, seenLensIds, lens);
		}
		return report;
	}

	private void validateLens(
			NormalizedTranscript transcript,
			Set<String> supportedLensIds,
			Set<String> seenLensIds,
			ReviewLensResult lens
	) {
		if (lens == null || isBlank(lens.lensId()) || !supportedLensIds.contains(lens.lensId())) {
			throw new MultiLensReviewException("Review report includes unsupported lens");
		}
		if (!seenLensIds.add(lens.lensId())) {
			throw new MultiLensReviewException("Review report includes duplicate lens: " + lens.lensId());
		}
		if (!"reviewed".equals(lens.status()) && !"insufficient_evidence".equals(lens.status())) {
			throw new MultiLensReviewException("Review lens has unsupported status: " + lens.lensId());
		}
		if ("insufficient_evidence".equals(lens.status()) && !nullToEmpty(lens.findings()).isEmpty()) {
			throw new MultiLensReviewException("Insufficient-evidence lens must not include findings: "
					+ lens.lensId());
		}
		for (ReviewFinding finding : nullToEmpty(lens.findings())) {
			validateFinding(transcript, lens.lensId(), finding);
		}
	}

	private void validateFinding(NormalizedTranscript transcript, String lensId, ReviewFinding finding) {
		if (finding == null || isBlank(finding.summary())) {
			throw new MultiLensReviewException("Review finding is missing a summary for lens: " + lensId);
		}
		if (nullToEmpty(finding.evidence()).isEmpty()) {
			throw new MultiLensReviewException("Review finding is missing transcript evidence for lens: " + lensId);
		}
		for (ReviewEvidenceReference evidence : finding.evidence()) {
			validateEvidence(transcript, evidence, lensId);
		}
	}

	private void validateEvidence(
			NormalizedTranscript transcript,
			ReviewEvidenceReference evidence,
			String lensId
	) {
		if (evidence == null || isBlank(evidence.quote())) {
			throw new MultiLensReviewException("Review lens has blank evidence quote: " + lensId);
		}
		boolean matchesTranscript = transcript.turns().stream().anyMatch(turn -> turnMatchesEvidence(turn, evidence));
		if (!matchesTranscript) {
			throw new MultiLensReviewException("Evidence quote was not found in transcript for lens: " + lensId);
		}
	}

	private boolean turnMatchesEvidence(TranscriptTurn turn, ReviewEvidenceReference evidence) {
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
				existingPaths.evaluationJson(),
				existingPaths.evaluationMarkdown(),
				runDirectory.resolve("multi-lens-review.json").toString(),
				runDirectory.resolve("multi-lens-review.md").toString(),
				existingPaths.observationsMarkdown()
		);
		return new RunMetadata(
				metadata.callId(),
				metadata.scenarioId(),
				metadata.runMode(),
				metadata.targetPhoneNumber(),
				metadata.retellCallId(),
				metadata.startedAt(),
				metadata.endedAt(),
				metadata.callDurationSeconds(),
				"multi_lens_review_completed",
				artifactPaths,
				metadata.analysis(),
				metadata.evaluation(),
				RuntimeReproducibilityMetadata.forCommand("multi-lens-review")
		);
	}

	private String buildMarkdown(MultiLensReviewReport report) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# Structured Multi-Lens Review").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("call_id: ").append(report.callId()).append(System.lineSeparator());
		markdown.append("scenario_id: ").append(report.scenarioId()).append(System.lineSeparator());
		markdown.append("review_id: ").append(report.reviewId()).append(System.lineSeparator());
		markdown.append("human_review_required: true").append(System.lineSeparator());
		markdown.append(System.lineSeparator());
		markdown.append("## Lenses").append(System.lineSeparator());
		for (ReviewLensResult lens : nullToEmpty(report.lenses())) {
			markdown.append("- ").append(lens.lensLabel()).append(" (`").append(lens.lensId()).append("`)")
					.append(System.lineSeparator());
			markdown.append("  - Status: ").append(lens.status()).append(System.lineSeparator());
			markdown.append("  - Summary: ").append(lens.summary()).append(System.lineSeparator());
			for (ReviewFinding finding : nullToEmpty(lens.findings())) {
				markdown.append("  - Finding: ").append(finding.summary()).append(System.lineSeparator());
				for (ReviewEvidenceReference evidence : nullToEmpty(finding.evidence())) {
					markdown.append("  - Evidence turn ")
							.append(evidence.turnIndex())
							.append(" [")
							.append(evidence.speaker())
							.append("]: ")
							.append(evidence.quote())
							.append(System.lineSeparator());
				}
			}
			for (String warning : nullToEmpty(lens.warnings())) {
				markdown.append("  - Warning: ").append(warning).append(System.lineSeparator());
			}
		}
		markdown.append(System.lineSeparator());
		markdown.append("## Notes").append(System.lineSeparator());
		for (String note : nullToEmpty(report.notes())) {
			markdown.append("- ").append(note).append(System.lineSeparator());
		}
		markdown.append("- Multi-lens review is advisory. A human reviewer owns final judgment.")
				.append(System.lineSeparator());
		return markdown.toString();
	}

	private String reviewId(OffsetDateTime generatedAt, String callId) {
		String timestamp = generatedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		int suffix = Math.abs(callId.hashCode());
		return "multi_lens_review_" + timestamp + "_" + Integer.toHexString(suffix);
	}

	private Path optionalPath(String path) {
		return isBlank(path) ? null : Path.of(path);
	}

	private <T> List<T> nullToEmpty(List<T> values) {
		return values == null ? List.of() : values;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
