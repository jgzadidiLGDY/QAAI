package com.qaai.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaai.config.QaaiProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "qaai.analysis.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiAnalysisClient implements AnalysisClient {

	private static final int MAX_ERROR_BODY_LENGTH = 500;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;

	@Autowired
	public OpenAiAnalysisClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, QaaiProperties properties) {
		this(restClientBuilder.baseUrl("https://api.openai.com")
						.requestFactory(requestFactory(properties.openai().analysisTimeout()))
						.build(),
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
	public AnalysisReport analyze(AnalysisRequest analysisRequest) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new AnalysisException("OPENAI_API_KEY is required for --analyze-call");
		}
		String prompt = analysisRequest.prompt();

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
					.onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
						String responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
						throw new AnalysisException(
								"Provider openai operation chat-completions failed with HTTP "
										+ clientResponse.getStatusCode().value()
										+ errorBody(responseBody)
						);
					})
					.body(ChatCompletionResponse.class);
			String content = responseContent(response);
			return objectMapper.readValue(content, AnalysisReport.class);
		} catch (AnalysisException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new AnalysisException("Provider openai operation chat-completions failed", exception);
		} catch (Exception exception) {
			throw new AnalysisException("Provider openai operation chat-completions returned invalid analysis JSON",
					exception);
		}
	}

	@Override
	public String provider() {
		return "openai";
	}

	@Override
	public String model() {
		return model;
	}

	private String responseContent(ChatCompletionResponse response) {
		if (response == null || response.choices() == null || response.choices().isEmpty()) {
			throw new AnalysisException("Provider openai operation chat-completions response did not include choices");
		}
		ChatCompletionChoice choice = response.choices().getFirst();
		if (choice.message() == null || choice.message().content() == null || choice.message().content().isBlank()) {
			throw new AnalysisException(
					"Provider openai operation chat-completions response did not include message content");
		}
		return choice.message().content();
	}

	private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(timeout);
		requestFactory.setReadTimeout(timeout);
		return requestFactory;
	}

	private String errorBody(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "";
		}
		String compactBody = responseBody.replaceAll("\\s+", " ").trim();
		if (compactBody.length() > MAX_ERROR_BODY_LENGTH) {
			compactBody = compactBody.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
		}
		return ": " + compactBody;
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
