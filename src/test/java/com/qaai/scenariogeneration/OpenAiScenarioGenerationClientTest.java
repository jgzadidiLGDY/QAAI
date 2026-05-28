package com.qaai.scenariogeneration;

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

class OpenAiScenarioGenerationClientTest {

	@Test
	void parsesStructuredScenarioGenerationResponse() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiScenarioGenerationClient client = new OpenAiScenarioGenerationClient(
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
						        "content": "{\\"coverage_plan_markdown\\":\\"# Plan\\",\\"scenarios\\":[{\\"id\\":\\"draft_001\\",\\"name\\":\\"Draft\\",\\"workflow\\":\\"appointment_scheduling\\",\\"persona\\":{\\"name\\":\\"Alex Patient\\",\\"date_of_birth\\":\\"1980-01-01\\",\\"phone_number\\":\\"+15555550100\\"},\\"goal\\":{\\"call_reason\\":\\"scheduling\\",\\"summary\\":\\"Schedule visit.\\",\\"expected_outcome\\":\\"Appointment next step.\\"},\\"constraints\\":{\\"allowed_facts\\":[\\"Synthetic fact.\\"],\\"disallowed_behavior\\":[\\"Do not provide real patient data.\\"]},\\"coverage\\":{\\"workflow_area\\":\\"appointment_scheduling\\",\\"edge_cases\\":[\\"happy_path\\"],\\"risk_focus\\":\\"Confirm a next step.\\"},\\"conversation_quality\\":{\\"welcome_behavior\\":\\"Start clearly.\\",\\"initiative\\":\\"One fact at a time.\\",\\"pacing\\":\\"Short turns.\\",\\"clarification\\":\\"Ask to rephrase.\\",\\"expected_risks\\":[\\"No confirmation.\\"]},\\"steps\\":[{\\"intent\\":\\"greeting\\",\\"patient_says\\":\\"Hi, I need an appointment.\\"}]}]}"
						      }
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		ScenarioGenerationDraftSet draftSet = client.generate(new ScenarioGenerationRequest(
				"generation_123",
				"medical office scheduling agent",
				1,
				"prompt text"
		));

		assertThat(draftSet.coveragePlanMarkdown()).isEqualTo("# Plan");
		assertThat(draftSet.scenarios()).hasSize(1);
		assertThat(draftSet.scenarios().getFirst().id()).isEqualTo("draft_001");
		server.verify();
	}

	@Test
	void classifiesHttpErrorsWithoutLeakingPrompt() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiScenarioGenerationClient client = new OpenAiScenarioGenerationClient(
				builder.build(),
				new ObjectMapper().findAndRegisterModules(),
				"test-api-key",
				"test-model"
		);

		server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
				.andRespond(withServerError().body("temporary provider failure"));

		assertThatThrownBy(() -> client.generate(new ScenarioGenerationRequest(
				"generation_123",
				"medical office scheduling agent",
				1,
				"full scenario generation prompt should not be echoed"
		)))
				.isInstanceOf(ScenarioGenerationException.class)
				.hasMessageContaining("Provider openai operation scenario-generation failed")
				.hasMessageContaining("HTTP 500")
				.hasMessageContaining("temporary provider failure")
				.hasMessageNotContaining("full scenario generation prompt");

		server.verify();
	}

	@Test
	void requiresOpenAiApiKey() {
		OpenAiScenarioGenerationClient client = new OpenAiScenarioGenerationClient(
				RestClient.builder().baseUrl("https://api.openai.test").build(),
				new ObjectMapper().findAndRegisterModules(),
				"",
				"test-model"
		);

		assertThatThrownBy(() -> client.generate(new ScenarioGenerationRequest(
				"generation_123",
				"medical office scheduling agent",
				1,
				"prompt text"
		)))
				.isInstanceOf(ScenarioGenerationException.class)
				.hasMessageContaining("OPENAI_API_KEY is required");
	}
}
