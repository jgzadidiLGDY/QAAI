package com.qaai.retell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RetellClientTest {

	@Test
	void createsPhoneCallWithBearerTokenAndScenarioMetadata() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RetellClient client = new RetellClient(builder.build(), "test-api-key");

		server.expect(requestTo("https://api.example.test/v2/create-phone-call"))
				.andExpect(method(POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
				.andExpect(content().json("""
						{
						  "from_number": "+15555550100",
						  "to_number": "+18054398008",
						  "override_agent_id": "agent_123",
						  "metadata": {
						    "qaai_call_id": "call_20260523_130000_test1234",
						    "scenario_id": "appointment_reschedule_001"
						  },
						  "retell_llm_dynamic_variables": {
						    "workflow": "appointment_rescheduling"
						  }
						}
						"""))
				.andRespond(withCreatedEntity(URI.create("https://api.example.test/v2/create-phone-call"))
						.contentType(MediaType.APPLICATION_JSON)
						.body("""
								{
								  "call_id": "retell_call_123",
								  "call_status": "registered",
								  "agent_id": "agent_123",
								  "from_number": "+15555550100",
								  "to_number": "+18054398008",
								  "direction": "outbound",
								  "metadata": {
								    "qaai_call_id": "call_20260523_130000_test1234"
								  }
								}
								"""));

		RetellOutboundCallResponse response = client.createPhoneCall(new RetellOutboundCallRequest(
				"+15555550100",
				"+18054398008",
				"agent_123",
				Map.of(
						"qaai_call_id", "call_20260523_130000_test1234",
						"scenario_id", "appointment_reschedule_001"
				),
				Map.of("workflow", "appointment_rescheduling")
		));

		assertThat(response.callId()).isEqualTo("retell_call_123");
		assertThat(response.callStatus()).isEqualTo("registered");
		assertThat(response.direction()).isEqualTo("outbound");
		server.verify();
	}

	@Test
	void throwsClearExceptionForFailedResponse() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RetellClient client = new RetellClient(builder.build(), "test-api-key");

		server.expect(requestTo("https://api.example.test/v2/create-phone-call"))
				.andRespond(withUnauthorizedRequest().body("invalid api key"));

		assertThatThrownBy(() -> client.createPhoneCall(new RetellOutboundCallRequest(
				"+15555550100",
				"+18054398008",
				"agent_123",
				Map.of(),
				Map.of()
		)))
				.isInstanceOf(RetellApiException.class)
				.hasMessageContaining("Provider retell operation create-phone-call failed")
				.hasMessageContaining("HTTP 401")
				.hasMessageContaining("invalid api key");

		server.verify();
	}

	@Test
	void classifiesMalformedCreatePhoneCallResponse() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RetellClient client = new RetellClient(builder.build(), "test-api-key");

		server.expect(requestTo("https://api.example.test/v2/create-phone-call"))
				.andRespond(withSuccess("", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.createPhoneCall(new RetellOutboundCallRequest(
				"+15555550100",
				"+18054398008",
				"agent_123",
				Map.of(),
				Map.of()
		)))
				.isInstanceOf(RetellApiException.class)
				.hasMessageContaining("Provider retell operation create-phone-call response did not include a body");

		server.verify();
	}

	@Test
	void retrievesCallDetailsWithTranscriptAndRecordingUrl() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RetellClient client = new RetellClient(builder.build(), "test-api-key");

		server.expect(requestTo("https://api.example.test/v2/get-call/retell_call_123"))
				.andExpect(method(GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
				.andRespond(withSuccess("""
						{
						  "call_id": "retell_call_123",
						  "call_status": "ended",
						  "recording_url": "https://recordings.example.test/recording.wav",
						  "transcript_object": [
						    {
						      "role": "agent",
						      "content": "Thanks for calling.",
						      "words": [{ "word": "Thanks", "start": 0.5, "end": 0.9 }]
						    },
						    {
						      "role": "user",
						      "content": "I need to reschedule.",
						      "words": [{ "word": "I", "start": 1.2, "end": 1.3 }]
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		RetellCallDetailsResponse response = client.getCall("retell_call_123");

		assertThat(response.callId()).isEqualTo("retell_call_123");
		assertThat(response.callStatus()).isEqualTo("ended");
		assertThat(response.recordingUrl()).isEqualTo("https://recordings.example.test/recording.wav");
		assertThat(response.transcriptObject()).hasSize(2);
		assertThat(response.transcriptObject().getFirst().words().getFirst().start()).isEqualTo(0.5);
		server.verify();
	}

	@Test
	void downloadsRecordingWithSeparateRecordingClient() {
		RestClient.Builder apiBuilder = RestClient.builder().baseUrl("https://api.example.test");
		RestClient.Builder recordingBuilder = RestClient.builder();
		MockRestServiceServer apiServer = MockRestServiceServer.bindTo(apiBuilder).build();
		MockRestServiceServer recordingServer = MockRestServiceServer.bindTo(recordingBuilder).build();
		RetellClient client = new RetellClient(apiBuilder.build(), recordingBuilder.build(), "test-api-key");

		recordingServer.expect(requestTo("https://recordings.example.test/recording.wav"))
				.andExpect(method(GET))
				.andRespond(withSuccess("audio-bytes", MediaType.APPLICATION_OCTET_STREAM));

		assertThat(client.downloadRecording("https://recordings.example.test/recording.wav"))
				.isEqualTo("audio-bytes".getBytes());

		apiServer.verify();
		recordingServer.verify();
	}

	@Test
	void classifiesRecordingDownloadHttpErrors() {
		RestClient.Builder recordingBuilder = RestClient.builder();
		MockRestServiceServer recordingServer = MockRestServiceServer.bindTo(recordingBuilder).build();
		RetellClient client = new RetellClient(RestClient.builder().build(), recordingBuilder.build(), "test-api-key");

		recordingServer.expect(requestTo("https://recordings.example.test/recording.wav"))
				.andRespond(withServerError().body("storage unavailable"));

		assertThatThrownBy(() -> client.downloadRecording("https://recordings.example.test/recording.wav"))
				.isInstanceOf(RetellApiException.class)
				.hasMessageContaining("Provider retell operation recording-download failed")
				.hasMessageContaining("HTTP 500")
				.hasMessageContaining("storage unavailable");

		recordingServer.verify();
	}

	@Test
	void requiresApiKey() {
		RetellClient client = new RetellClient(RestClient.builder().build(), "");

		assertThatThrownBy(() -> client.createPhoneCall(new RetellOutboundCallRequest(
				"+15555550100",
				"+18054398008",
				"agent_123",
				Map.of(),
				Map.of()
		)))
				.isInstanceOf(RetellApiException.class)
				.hasMessageContaining("RETELL_API_KEY is required");
	}
}
