package com.qaai.runner;

import com.qaai.artifacts.ArtifactManifest;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.NormalizedTranscript;
import com.qaai.artifacts.RunMetadata;
import com.qaai.artifacts.TranscriptTurn;
import com.qaai.retell.RetellApiException;
import com.qaai.retell.RetellCallDetailsResponse;
import com.qaai.retell.RetellClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArtifactCaptureService {

	private final ArtifactWriter artifactWriter;
	private final RetellClient retellClient;
	private final Clock clock;

	@Autowired
	public ArtifactCaptureService(ArtifactWriter artifactWriter, RetellClient retellClient) {
		this(artifactWriter, retellClient, Clock.systemDefaultZone());
	}

	public ArtifactCaptureService(ArtifactWriter artifactWriter, RetellClient retellClient, Clock clock) {
		this.artifactWriter = artifactWriter;
		this.retellClient = retellClient;
		this.clock = clock;
	}

	public ArtifactCaptureResult capture(String callId) {
		if (isBlank(callId)) {
			throw new IllegalArgumentException("Provide --call-id=<local_call_id> with --capture-artifacts");
		}

		RunMetadata existingMetadata = artifactWriter.readMetadata(callId);
		validateMetadata(callId, existingMetadata);

		RetellCallDetailsResponse callDetails = retellClient.getCall(existingMetadata.retellCallId());
		NormalizedTranscript transcript = normalizeTranscript(existingMetadata, callDetails);
		String transcriptText = buildTranscriptText(transcript, callDetails);
		AudioCapture audioCapture = downloadAudioIfAvailable(callDetails);
		RunMetadata updatedMetadata = updateMetadata(existingMetadata, callDetails, transcript, audioCapture);
		ArtifactManifest manifest = buildManifest(updatedMetadata, callDetails, transcript, audioCapture);

		artifactWriter.writeCapturedArtifacts(
				callId,
				updatedMetadata,
				transcript,
				transcriptText,
				audioCapture.bytes(),
				manifest
		);

		Path runDirectory = artifactWriter.runDirectory(callId);
		return new ArtifactCaptureResult(
				updatedMetadata,
				runDirectory,
				runDirectory.resolve("transcript.json"),
				runDirectory.resolve("transcript.txt"),
				audioCapture.present() ? runDirectory.resolve("audio.wav") : null,
				runDirectory.resolve("manifest.json")
		);
	}

	private void validateMetadata(String callId, RunMetadata metadata) {
		if (!callId.equals(metadata.callId())) {
			throw new IllegalArgumentException("metadata.json call_id does not match requested call_id: " + callId);
		}
		if (!"retell".equals(metadata.runMode())) {
			throw new IllegalArgumentException("Artifact capture requires metadata with run_mode=retell");
		}
		if (isBlank(metadata.retellCallId())) {
			throw new IllegalArgumentException("Artifact capture requires metadata.retell_call_id");
		}
	}

	private NormalizedTranscript normalizeTranscript(RunMetadata metadata, RetellCallDetailsResponse callDetails) {
		List<TranscriptTurn> turns = new ArrayList<>();
		List<RetellCallDetailsResponse.TranscriptObjectTurn> retellTurns = callDetails == null
				? List.of()
				: nullToEmpty(callDetails.transcriptObject());

		for (int index = 0; index < retellTurns.size(); index++) {
			RetellCallDetailsResponse.TranscriptObjectTurn retellTurn = retellTurns.get(index);
			turns.add(new TranscriptTurn(
					index + 1,
					normalizeSpeaker(retellTurn.role()),
					retellTurn.content(),
					firstTimestamp(retellTurn)
			));
		}

		if (turns.isEmpty() && callDetails != null && !isBlank(callDetails.transcript())) {
			turns.add(new TranscriptTurn(1, "unknown", callDetails.transcript(), null));
		}

		return new NormalizedTranscript(metadata.callId(), metadata.scenarioId(), "retell", turns);
	}

	private String buildTranscriptText(NormalizedTranscript transcript, RetellCallDetailsResponse callDetails) {
		StringBuilder text = new StringBuilder();
		text.append("Retell Transcript").append(System.lineSeparator());
		text.append("call_id: ").append(transcript.callId()).append(System.lineSeparator());
		text.append("scenario_id: ").append(transcript.scenarioId()).append(System.lineSeparator());
		text.append("source: retell").append(System.lineSeparator());
		if (callDetails != null && !isBlank(callDetails.callStatus())) {
			text.append("retell_status: ").append(callDetails.callStatus()).append(System.lineSeparator());
		}
		text.append(System.lineSeparator());

		if (transcript.turns().isEmpty()) {
			text.append("No transcript turns were available from Retell at capture time.")
					.append(System.lineSeparator());
			return text.toString();
		}

		for (TranscriptTurn turn : transcript.turns()) {
			text.append(turn.index())
					.append(". [")
					.append(turn.speaker())
					.append("] ")
					.append(turn.text())
					.append(System.lineSeparator());
		}

		return text.toString();
	}

	private AudioCapture downloadAudioIfAvailable(RetellCallDetailsResponse callDetails) {
		if (callDetails == null || isBlank(callDetails.recordingUrl())) {
			return new AudioCapture(null, "Retell did not provide a recording_url at capture time.");
		}
		try {
			byte[] audioBytes = retellClient.downloadRecording(callDetails.recordingUrl());
			if (audioBytes == null || audioBytes.length == 0) {
				return new AudioCapture(null, "Retell provided a recording_url, but audio was not downloaded.");
			}
			return new AudioCapture(audioBytes, "Downloaded from Retell recording_url.");
		} catch (RetellApiException exception) {
			return new AudioCapture(null, "Audio download failed: " + exception.getMessage());
		}
	}

	private RunMetadata updateMetadata(
			RunMetadata existingMetadata,
			RetellCallDetailsResponse callDetails,
			NormalizedTranscript transcript,
			AudioCapture audioCapture
	) {
		Path runDirectory = artifactWriter.runDirectory(existingMetadata.callId());
		ArtifactPaths artifactPaths = new ArtifactPaths(
				pathIfExists(existingMetadata.artifactPaths().scenario(), runDirectory.resolve("scenario.yaml")),
				runDirectory.resolve("metadata.json").toString(),
				runDirectory.resolve("transcript.txt").toString(),
				runDirectory.resolve("transcript.json").toString(),
				pathIfExists(existingMetadata.artifactPaths().patientSimulation(),
						runDirectory.resolve("patient_simulation.md")),
				audioCapture.present() ? runDirectory.resolve("audio.wav").toString() : null,
				runDirectory.resolve("manifest.json").toString(),
				existingMetadata.artifactPaths().analysisJson(),
				existingMetadata.artifactPaths().analysisMarkdown(),
				pathIfExists(existingMetadata.artifactPaths().observationsMarkdown(), runDirectory.resolve("observations.md"))
		);

		return new RunMetadata(
				existingMetadata.callId(),
				existingMetadata.scenarioId(),
				existingMetadata.runMode(),
				existingMetadata.targetPhoneNumber(),
				existingMetadata.retellCallId(),
				existingMetadata.startedAt(),
				OffsetDateTime.now(clock),
				captureStatus(callDetails, transcript, audioCapture),
				artifactPaths
		);
	}

	private ArtifactManifest buildManifest(
			RunMetadata metadata,
			RetellCallDetailsResponse callDetails,
			NormalizedTranscript transcript,
			AudioCapture audioCapture
	) {
		Path runDirectory = artifactWriter.runDirectory(metadata.callId());
		List<ArtifactManifest.ArtifactEntry> artifacts = List.of(
				entry("scenario", metadata.artifactPaths().scenario()),
				generatedEntry("metadata", metadata.artifactPaths().metadata()),
				generatedEntry("transcript_json", metadata.artifactPaths().transcriptJson()),
				generatedEntry("transcript_text", metadata.artifactPaths().transcriptText()),
				entry("patient_simulation", metadata.artifactPaths().patientSimulation()),
				new ArtifactManifest.ArtifactEntry(
						"audio",
						metadata.artifactPaths().audio(),
						audioCapture.present(),
						audioCapture.note()
				),
				entry("observations_markdown", metadata.artifactPaths().observationsMarkdown()),
				new ArtifactManifest.ArtifactEntry(
						"manifest",
						runDirectory.resolve("manifest.json").toString(),
						true,
						"Generated during Phase 4 artifact capture."
				)
		);

		String transcriptNote = transcript.turns().isEmpty()
				? "No transcript turns were available from Retell at capture time."
				: null;

		if (transcriptNote != null) {
			artifacts = new ArrayList<>(artifacts);
			artifacts.set(2, new ArtifactManifest.ArtifactEntry(
					"transcript_json",
					metadata.artifactPaths().transcriptJson(),
					true,
					transcriptNote
			));
			artifacts.set(3, new ArtifactManifest.ArtifactEntry(
					"transcript_text",
					metadata.artifactPaths().transcriptText(),
					true,
					transcriptNote
			));
		}

		return new ArtifactManifest(
				metadata.callId(),
				metadata.scenarioId(),
				metadata.retellCallId(),
				OffsetDateTime.now(clock),
				callDetails == null ? null : callDetails.callStatus(),
				recordingUrl(callDetails),
				artifacts
		);
	}

	private ArtifactManifest.ArtifactEntry entry(String name, String path) {
		return new ArtifactManifest.ArtifactEntry(name, path, path != null && Files.exists(Path.of(path)), null);
	}

	private ArtifactManifest.ArtifactEntry generatedEntry(String name, String path) {
		return new ArtifactManifest.ArtifactEntry(name, path, path != null, "Generated during Phase 4 artifact capture.");
	}

	private String captureStatus(
			RetellCallDetailsResponse callDetails,
			NormalizedTranscript transcript,
			AudioCapture audioCapture
	) {
		if (callDetails == null) {
			return "artifacts_capture_failed";
		}
		if (transcript.turns().isEmpty() || !audioCapture.present()) {
			return "artifacts_partially_captured";
		}
		return "artifacts_captured";
	}

	private String normalizeSpeaker(String role) {
		if ("agent".equalsIgnoreCase(role)) {
			return "patient";
		}
		if ("user".equalsIgnoreCase(role)) {
			return "receptionist";
		}
		if (isBlank(role)) {
			return "unknown";
		}
		return role;
	}

	private Double firstTimestamp(RetellCallDetailsResponse.TranscriptObjectTurn turn) {
		if (turn.words() == null || turn.words().isEmpty()) {
			return null;
		}
		return turn.words().getFirst().start();
	}

	private String pathIfExists(String existingPath, Path defaultPath) {
		if (!isBlank(existingPath)) {
			return existingPath;
		}
		return defaultPath.toString();
	}

	private String recordingUrl(RetellCallDetailsResponse callDetails) {
		return callDetails == null ? null : callDetails.recordingUrl();
	}

	private <T> List<T> nullToEmpty(List<T> values) {
		return values == null ? List.of() : values;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
