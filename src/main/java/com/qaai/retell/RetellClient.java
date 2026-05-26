package com.qaai.retell;

import com.qaai.config.QaaiProperties;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RetellClient {

	private static final int MAX_ERROR_BODY_LENGTH = 500;

	private final RestClient restClient;
	private final RestClient recordingRestClient;
	private final String apiKey;

	@Autowired
	public RetellClient(QaaiProperties properties, RestClient.Builder restClientBuilder) {
		this(
				restClientBuilder.baseUrl(properties.retell().baseUrl())
						.requestFactory(requestFactory(properties.retell().apiTimeout()))
						.build(),
				RestClient.builder()
						.requestFactory(requestFactory(properties.retell().recordingDownloadTimeout()))
						.build(),
				properties.retell().apiKey()
		);
	}

	public RetellClient(RestClient restClient, String apiKey) {
		this(restClient, restClient, apiKey);
	}

	public RetellClient(RestClient restClient, RestClient recordingRestClient, String apiKey) {
		this.restClient = restClient;
		this.recordingRestClient = recordingRestClient;
		this.apiKey = apiKey;
	}

	public RetellOutboundCallResponse createPhoneCall(RetellOutboundCallRequest request) {
		if (isBlank(apiKey)) {
			throw new RetellApiException("RETELL_API_KEY is required for --run-mode=retell");
		}

		try {
			RetellOutboundCallResponse response = restClient.post()
					.uri("/v2/create-phone-call")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.body(request)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
						String responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
						throw new RetellApiException(
								"Provider retell operation create-phone-call failed with HTTP "
										+ clientResponse.getStatusCode().value()
										+ errorBody(responseBody)
						);
					})
					.body(RetellOutboundCallResponse.class);
			if (response == null) {
				throw new RetellApiException(
						"Provider retell operation create-phone-call response did not include a body");
			}
			return response;
		} catch (RetellApiException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new RetellApiException("Provider retell operation create-phone-call failed", exception);
		}
	}

	public RetellCallDetailsResponse getCall(String retellCallId) {
		if (isBlank(apiKey)) {
			throw new RetellApiException("RETELL_API_KEY is required for artifact capture");
		}
		if (isBlank(retellCallId)) {
			throw new RetellApiException("retell_call_id is required for artifact capture");
		}

		try {
			RetellCallDetailsResponse response = restClient.get()
					.uri("/v2/get-call/{call_id}", retellCallId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (httpRequest, clientResponse) -> {
						String responseBody = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
						throw new RetellApiException(
								"Provider retell operation get-call failed with HTTP "
										+ clientResponse.getStatusCode().value()
										+ errorBody(responseBody)
						);
					})
					.body(RetellCallDetailsResponse.class);
			if (response == null) {
				throw new RetellApiException("Provider retell operation get-call response did not include a body");
			}
			return response;
		} catch (RetellApiException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new RetellApiException("Provider retell operation get-call failed", exception);
		}
	}

	public byte[] downloadRecording(String recordingUrl) {
		if (isBlank(recordingUrl)) {
			throw new RetellApiException("recording_url is required to download audio");
		}

		try {
			byte[] audioBytes = recordingRestClient.get()
					.uri(URI.create(recordingUrl))
					.retrieve()
					.onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
						String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
						throw new RetellApiException(
								"Provider retell operation recording-download failed with HTTP "
										+ response.getStatusCode().value()
										+ errorBody(responseBody)
						);
					})
					.body(byte[].class);
			if (audioBytes == null) {
				throw new RetellApiException(
						"Provider retell operation recording-download response did not include a body");
			}
			return audioBytes;
		} catch (RetellApiException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new RetellApiException("Provider retell operation recording-download failed", exception);
		}
	}

	private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(timeout);
		requestFactory.setReadTimeout(timeout);
		return requestFactory;
	}

	private String errorBody(String responseBody) {
		if (isBlank(responseBody)) {
			return "";
		}
		String compactBody = responseBody.replaceAll("\\s+", " ").trim();
		if (compactBody.length() > MAX_ERROR_BODY_LENGTH) {
			compactBody = compactBody.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
		}
		return ": " + compactBody;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
