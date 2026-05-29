package com.qaai.artifacts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ArtifactCompletenessChecker {

	public ArtifactCompleteness check(RunMetadata metadata) {
		if (metadata == null) {
			throw new IllegalArgumentException("metadata is required for artifact completeness checks");
		}
		if (metadata.artifactPaths() == null) {
			throw new IllegalArgumentException("metadata.artifact_paths is required for artifact completeness checks");
		}

		Set<String> required = requiredArtifacts(metadata);
		List<ArtifactCompleteness.ArtifactStatus> artifactStatuses = new ArrayList<>();
		List<String> missingRequiredArtifacts = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		addStatus(artifactStatuses, missingRequiredArtifacts, required, "scenario", metadata.artifactPaths().scenario());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "metadata", metadata.artifactPaths().metadata());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "transcript_text",
				metadata.artifactPaths().transcriptText());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "transcript_json",
				metadata.artifactPaths().transcriptJson());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "patient_simulation",
				metadata.artifactPaths().patientSimulation());
		addAudioStatus(artifactStatuses, warnings, metadata, metadata.artifactPaths().audio());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "manifest", metadata.artifactPaths().manifest());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "analysis_json",
				metadata.artifactPaths().analysisJson());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "analysis_markdown",
				metadata.artifactPaths().analysisMarkdown());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "evaluation_json",
				metadata.artifactPaths().evaluationJson());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "evaluation_markdown",
				metadata.artifactPaths().evaluationMarkdown());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "multi_lens_review_json",
				metadata.artifactPaths().multiLensReviewJson());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "multi_lens_review_markdown",
				metadata.artifactPaths().multiLensReviewMarkdown());
		addStatus(artifactStatuses, missingRequiredArtifacts, required, "observations_markdown",
				metadata.artifactPaths().observationsMarkdown());

		return new ArtifactCompleteness(
				missingRequiredArtifacts.isEmpty(),
				List.copyOf(artifactStatuses),
				List.copyOf(missingRequiredArtifacts),
				List.copyOf(warnings)
		);
	}

	private Set<String> requiredArtifacts(RunMetadata metadata) {
		Set<String> required = new LinkedHashSet<>();
		required.add("scenario");
		required.add("metadata");
		required.add("patient_simulation");
		required.add("observations_markdown");

		if ("dry_run".equals(metadata.runMode()) || "text_chat".equals(metadata.runMode())) {
			required.add("transcript_text");
		}
		if ("text_chat".equals(metadata.runMode())) {
			required.add("transcript_json");
		}
		if (isCaptureStatus(metadata.status()) || "analysis_completed".equals(metadata.status())) {
			required.add("transcript_text");
			required.add("transcript_json");
			addManifestIfVoice(metadata, required);
		}
		if ("analysis_completed".equals(metadata.status())) {
			required.add("analysis_json");
			required.add("analysis_markdown");
		}
		if ("evaluation_completed".equals(metadata.status())) {
			required.add("transcript_text");
			required.add("transcript_json");
			addManifestIfVoice(metadata, required);
			required.add("evaluation_json");
			required.add("evaluation_markdown");
		}
		if ("multi_lens_review_completed".equals(metadata.status())) {
			required.add("transcript_text");
			required.add("transcript_json");
			addManifestIfVoice(metadata, required);
			required.add("multi_lens_review_json");
			required.add("multi_lens_review_markdown");
		}
		return required;
	}

	private void addManifestIfVoice(RunMetadata metadata, Set<String> required) {
		if (!"text".equals(metadata.channel())) {
			required.add("manifest");
		}
	}

	private boolean isCaptureStatus(String status) {
		return "artifacts_captured".equals(status)
				|| "artifacts_partially_captured".equals(status)
				|| "artifacts_capture_failed".equals(status);
	}

	private void addStatus(
			List<ArtifactCompleteness.ArtifactStatus> artifactStatuses,
			List<String> missingRequiredArtifacts,
			Set<String> required,
			String name,
			String path
	) {
		boolean isRequired = required.contains(name);
		boolean present = exists(path);
		String note = null;
		if (isRequired && !present) {
			missingRequiredArtifacts.add(name);
			note = "Required artifact is missing.";
		}
		artifactStatuses.add(new ArtifactCompleteness.ArtifactStatus(name, path, isRequired, present, note));
	}

	private void addAudioStatus(
			List<ArtifactCompleteness.ArtifactStatus> artifactStatuses,
			List<String> warnings,
			RunMetadata metadata,
			String path
	) {
		boolean present = exists(path);
		String note = null;
		if (!present && expectsCapturedArtifacts(metadata)) {
			note = "Audio is optional and may be unavailable from Retell.";
			warnings.add("audio missing or unavailable");
		}
		artifactStatuses.add(new ArtifactCompleteness.ArtifactStatus("audio", path, false, present, note));
	}

	private boolean expectsCapturedArtifacts(RunMetadata metadata) {
		if ("text".equals(metadata.channel())) {
			return false;
		}
		return isCaptureStatus(metadata.status())
				|| "analysis_completed".equals(metadata.status())
				|| "evaluation_completed".equals(metadata.status())
				|| "multi_lens_review_completed".equals(metadata.status());
	}

	private boolean exists(String path) {
		return path != null && Files.exists(Path.of(path));
	}
}
