package com.qaai.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiAnalysisClientTest {

	@Test
	void parsesStructuredAnalysisResponse() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiAnalysisClient client = new OpenAiAnalysisClient(
				builder.build(),
				new ObjectMapper().findAndRegisterModules(),
				"test-api-key",
				"test-model"
		);

		server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
				.andExpect(method(POST))
				.andExpect(header("Authorization", "Bearer test-api-key"))
				.andRespond(withSuccess("""
						{
						  "choices": [
						    {
						      "message": {
						        "content": "{\\"call_id\\":\\"call_123\\",\\"scenario_id\\":\\"scenario_123\\",\\"summary\\":\\"Call completed.\\",\\"human_review_required\\":true,\\"findings\\":[],\\"notes\\":[\\"Review manually.\\"]}"
						      }
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		AnalysisReport report = client.analyze("prompt text");

		assertThat(report.callId()).isEqualTo("call_123");
		assertThat(report.scenarioId()).isEqualTo("scenario_123");
		assertThat(report.humanReviewRequired()).isTrue();
		assertThat(report.notes()).containsExactly("Review manually.");
		server.verify();
	}

	@Test
	void classifiesHttpErrorsWithoutLeakingPrompt() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiAnalysisClient client = new OpenAiAnalysisClient(
				builder.build(),
				new ObjectMapper().findAndRegisterModules(),
				"test-api-key",
				"test-model"
		);

		server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
				.andRespond(withServerError().body("temporary provider failure"));

		assertThatThrownBy(() -> client.analyze("full transcript prompt should not be echoed"))
				.isInstanceOf(AnalysisException.class)
				.hasMessageContaining("Provider openai operation chat-completions failed")
				.hasMessageContaining("HTTP 500")
				.hasMessageContaining("temporary provider failure")
				.hasMessageNotContaining("full transcript prompt");

		server.verify();
	}

	@Test
	void classifiesMissingMessageContent() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiAnalysisClient client = new OpenAiAnalysisClient(
				builder.build(),
				new ObjectMapper().findAndRegisterModules(),
				"test-api-key",
				"test-model"
		);

		server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
				.andRespond(withSuccess("""
						{
						  "choices": [
						    {
						      "message": {
						        "content": ""
						      }
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.analyze("prompt text"))
				.isInstanceOf(AnalysisException.class)
				.hasMessageContaining("response did not include message content");

		server.verify();
	}
}
