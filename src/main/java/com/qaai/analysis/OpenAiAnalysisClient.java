package com.qaai.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.config.QaaiProperties;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiAnalysisClient implements AnalysisClient {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;

	@Autowired
	public OpenAiAnalysisClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, QaaiProperties properties) {
		this(restClientBuilder.baseUrl("https://api.openai.com").build(),
				objectMapper.findAndRegisterModules(),
				properties.openai().apiKey(),
				properties.openai().analysisModel());
	}

	OpenAiAnalysisClient(RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
		this.apiKey = apiKey;
		this.model = model;
	}

	@Override
	public AnalysisReport analyze(String prompt) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new AnalysisException("OPENAI_API_KEY is required for --analyze-call");
		}

		Map<String, Object> request = Map.of(
				"model", model,
				"response_format", Map.of("type", "json_object"),
				"messages", List.of(
						Map.of("role", "system", "content", "Return only JSON. Cite evidence exactly."),
						Map.of("role", "user", "content", prompt)
				)
		);

		try {
			ChatCompletionResponse response = restClient.post()
					.uri("/v1/chat/completions")
					.header("Authorization", "Bearer " + apiKey)
					.body(request)
					.retrieve()
					.body(ChatCompletionResponse.class);
			String content = responseContent(response);
			return objectMapper.readValue(content, AnalysisReport.class);
		} catch (RestClientException exception) {
			throw new AnalysisException("OpenAI analysis request failed", exception);
		} catch (Exception exception) {
			throw new AnalysisException("OpenAI analysis response was not valid analysis JSON", exception);
		}
	}

	private String responseContent(ChatCompletionResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			throw new AnalysisException("OpenAI analysis response did not include choices");
		}
		ChatCompletionChoice choice = response.choices().getFirst();
		if (choice.message() == null || choice.message().content() == null || choice.message().content().isBlank()) {
			throw new AnalysisException("OpenAI analysis response did not include message content");
		}
		return choice.message().content();
	}

	private record ChatCompletionResponse(List<ChatCompletionChoice> choices) {
	}

	private record ChatCompletionChoice(ChatCompletionMessage message) {
	}

	private record ChatCompletionMessage(
			String role,
			@JsonProperty("content")
			String content
	) {
	}
}
