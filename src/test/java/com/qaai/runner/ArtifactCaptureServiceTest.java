package com.qaai.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.artifacts.ArtifactPaths;
import com.qaai.artifacts.ArtifactWriter;
import com.qaai.artifacts.RunMetadata;
import com.qaai.retell.RetellApiException;
import com.qaai.retell.RetellCallDetailsResponse;
import com.qaai.retell.RetellClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.RestClient;

class ArtifactCaptureServiceTest {

	@TempDir
	private Path tempDir;

	@Test
	void capturesRetellArtifactsForExistingCallId() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		Path scenarioPath = runDirectory.resolve("scenario.yaml");
		Files.writeString(scenarioPath, "id: appointment_reschedule_001%n".formatted());
		Files.writeString(runDirectory.resolve("observations.md"), "# Retell Call Start Observations%n".formatted());

		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		writer.writeCallStartedArtifacts(
				callId,
				scenarioPath,
				retellMetadata(callId, runDirectory),
				"# Patient Simulation Scenario%n".formatted(),
				"# Retell Call Start Observations%n".formatted()
		);
		ArtifactCaptureService service = new ArtifactCaptureService(
				writer,
				retellClient(callDetailsWithRecording(), "audio-bytes".getBytes()),
				Clock.fixed(Instant.parse("2026-05-23T18:30:00Z"), ZoneOffset.ofHours(-4))
		);

		ArtifactCaptureResult result = service.capture(callId);

		assertThat(result.metadata().status()).isEqualTo("artifacts_captured");
		assertThat(result.transcriptJson()).exists();
		assertThat(result.transcriptText()).exists();
		assertThat(result.audio()).exists();
		assertThat(result.manifest()).exists();
		assertThat(Files.readString(result.transcriptText())).contains(
				"Retell Transcript",
				"1. [patient] Thanks for calling.",
				"2. [receptionist] I need to reschedule."
		);
		assertThat(Files.readString(result.transcriptJson())).contains(
				"\"speaker\" : \"receptionist\"",
				"\"timestamp\" : 1.2"
		);
		assertThat(Files.readString(result.manifest())).contains(
				"\"name\" : \"audio\"",
				"\"present\" : true"
		);
		assertThat(Files.readString(runDirectory.resolve("metadata.json"))).contains(
				"\"status\" : \"artifacts_captured\"",
				"\"transcript_json\"",
				"\"audio\"",
				"\"manifest\"",
				"\"command\" : \"capture-artifacts\"",
				"\"app_version\" : \"0.0.1-SNAPSHOT\""
		);
	}

	@Test
	void marksCapturePartialWhenRetellHasNoRecordingUrl() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		Path scenarioPath = runDirectory.resolve("scenario.yaml");
		Files.writeString(scenarioPath, "id: appointment_reschedule_001%n".formatted());

		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		writer.writeCallStartedArtifacts(
				callId,
				scenarioPath,
				retellMetadata(callId, runDirectory),
				"# Patient Simulation Scenario%n".formatted(),
				"# Retell Call Start Observations%n".formatted()
		);
		ArtifactCaptureService service = new ArtifactCaptureService(
				writer,
				retellClient(callDetailsWithoutRecording(), null),
				Clock.fixed(Instant.parse("2026-05-23T18:30:00Z"), ZoneOffset.ofHours(-4))
		);

		ArtifactCaptureResult result = service.capture(callId);

		assertThat(result.metadata().status()).isEqualTo("artifacts_partially_captured");
		assertThat(result.audio()).isNull();
		assertThat(runDirectory.resolve("audio.wav")).doesNotExist();
		assertThat(Files.readString(result.manifest())).contains(
				"Retell did not provide a recording_url at capture time.",
				"\"present\" : false"
		);
	}

	@Test
	void fallsBackToRawTranscriptWhenStructuredTranscriptIsMissing() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		Path scenarioPath = runDirectory.resolve("scenario.yaml");
		Files.writeString(scenarioPath, "id: appointment_reschedule_001%n".formatted());

		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		writer.writeCallStartedArtifacts(
				callId,
				scenarioPath,
				retellMetadata(callId, runDirectory),
				"# Patient Simulation Scenario%n".formatted(),
				"# Retell Call Start Observations%n".formatted()
		);
		ArtifactCaptureService service = new ArtifactCaptureService(
				writer,
				retellClient(callDetailsWithRawTranscriptOnly(), "audio-bytes".getBytes()),
				Clock.fixed(Instant.parse("2026-05-23T18:30:00Z"), ZoneOffset.ofHours(-4))
		);

		ArtifactCaptureResult result = service.capture(callId);

		assertThat(result.metadata().status()).isEqualTo("artifacts_captured");
		assertThat(Files.readString(result.transcriptText())).contains(
				"1. [unknown] Agent: Thanks for calling.%nPatient: I need to reschedule.".formatted()
		);
		assertThat(Files.readString(result.transcriptJson())).contains(
				"\"speaker\" : \"unknown\"",
				"Agent: Thanks for calling."
		);
	}

	@Test
	void recordsAudioDownloadFailureInManifest() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		Path scenarioPath = runDirectory.resolve("scenario.yaml");
		Files.writeString(scenarioPath, "id: appointment_reschedule_001%n".formatted());

		ArtifactWriter writer = new ArtifactWriter(new ObjectMapper(), outputs);
		writer.writeCallStartedArtifacts(
				callId,
				scenarioPath,
				retellMetadata(callId, runDirectory),
				"# Patient Simulation Scenario%n".formatted(),
				"# Retell Call Start Observations%n".formatted()
		);
		ArtifactCaptureService service = new ArtifactCaptureService(
				writer,
				retellClientWithDownloadFailure(callDetailsWithRecording()),
				Clock.fixed(Instant.parse("2026-05-23T18:30:00Z"), ZoneOffset.ofHours(-4))
		);

		ArtifactCaptureResult result = service.capture(callId);

		assertThat(result.metadata().status()).isEqualTo("artifacts_partially_captured");
		assertThat(result.audio()).isNull();
		assertThat(Files.readString(result.manifest())).contains(
				"Audio download failed: recording unavailable",
				"\"name\" : \"audio\"",
				"\"present\" : false"
		);
	}

	@Test
	void rejectsDryRunMetadata() throws Exception {
		Path outputs = tempDir.resolve("outputs");
		String callId = "call_20260523_130000_test1234";
		Path runDirectory = outputs.resolve(callId);
		Files.createDirectories(runDirectory);
		new ObjectMapper().findAndRegisterModules().writerWithDefaultPrettyPrinter().writeValue(
				runDirectory.resolve("metadata.json").toFile(),
				new RunMetadata(
						callId,
						"appointment_reschedule_001",
						"dry_run",
						"+18054398008",
						null,
						OffsetDateTime.parse("2026-05-23T13:00:00-04:00"),
						OffsetDateTime.parse("2026-05-23T13:00:01-04:00"),
						"completed",
						new ArtifactPaths(
								runDirectory.resolve("scenario.yaml").toString(),
								runDirectory.resolve("metadata.json").toString(),
								runDirectory.resolve("transcript.txt").toString(),
								null,
								runDirectory.resolve("patient_simulation.md").toString(),
								null,
								null,
								null,
								null,
								null
						)
				)
		);

		ArtifactCaptureService service = new ArtifactCaptureService(
				new ArtifactWriter(new ObjectMapper(), outputs),
				retellClient(callDetailsWithRecording(), null)
		);

		assertThatThrownBy(() -> service.capture(callId))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("run_mode=retell");
	}

	private RunMetadata retellMetadata(String callId, Path runDirectory) {
		return new RunMetadata(
				callId,
				"appointment_reschedule_001",
				"retell",
				"+18054398008",
				"retell_call_123",
				OffsetDateTime.parse("2026-05-23T13:00:00-04:00"),
				OffsetDateTime.parse("2026-05-23T13:00:01-04:00"),
				"retell_registered",
				new ArtifactPaths(
						runDirectory.resolve("scenario.yaml").toString(),
						runDirectory.resolve("metadata.json").toString(),
						null,
						null,
						runDirectory.resolve("patient_simulation.md").toString(),
						null,
						null,
						null,
						null,
						runDirectory.resolve("observations.md").toString()
				)
		);
	}

	private RetellCallDetailsResponse callDetailsWithRecording() {
		return new RetellCallDetailsResponse(
				"retell_call_123",
				"ended",
				"agent_123",
				"+15555550100",
				"+18054398008",
				"outbound",
				Map.of("qaai_call_id", "call_20260523_130000_test1234"),
				null,
				List.of(
						new RetellCallDetailsResponse.TranscriptObjectTurn(
								"agent",
								"Thanks for calling.",
								List.of(new RetellCallDetailsResponse.TranscriptWord("Thanks", 0.5, 0.9))
						),
						new RetellCallDetailsResponse.TranscriptObjectTurn(
								"user",
								"I need to reschedule.",
								List.of(new RetellCallDetailsResponse.TranscriptWord("I", 1.2, 1.3))
						)
				),
				"https://recordings.example.test/recording.wav",
				10000L
		);
	}

	private RetellCallDetailsResponse callDetailsWithoutRecording() {
		return new RetellCallDetailsResponse(
				"retell_call_123",
				"ended",
				"agent_123",
				"+15555550100",
				"+18054398008",
				"outbound",
				Map.of(),
				null,
				callDetailsWithRecording().transcriptObject(),
				null,
				10000L
		);
	}

	private RetellCallDetailsResponse callDetailsWithRawTranscriptOnly() {
		return new RetellCallDetailsResponse(
				"retell_call_123",
				"ended",
				"agent_123",
				"+15555550100",
				"+18054398008",
				"outbound",
				Map.of(),
				"Agent: Thanks for calling.%nPatient: I need to reschedule.".formatted(),
				null,
				"https://recordings.example.test/recording.wav",
				10000L
		);
	}

	private RetellClient retellClient(RetellCallDetailsResponse callDetails, byte[] audioBytes) {
		return new RetellClient(RestClient.builder().build(), "unused") {
			@Override
			public RetellCallDetailsResponse getCall(String retellCallId) {
				return callDetails;
			}

			@Override
			public byte[] downloadRecording(String recordingUrl) {
				return audioBytes;
			}
		};
	}

	private RetellClient retellClientWithDownloadFailure(RetellCallDetailsResponse callDetails) {
		return new RetellClient(RestClient.builder().build(), "unused") {
			@Override
			public RetellCallDetailsResponse getCall(String retellCallId) {
				return callDetails;
			}

			@Override
			public byte[] downloadRecording(String recordingUrl) {
				throw new RetellApiException("recording unavailable");
			}
		};
	}
}
