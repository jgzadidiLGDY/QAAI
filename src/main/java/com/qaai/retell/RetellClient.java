package com.qaai.retell;

import com.qaai.config.QaaiProperties;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RetellClient {

	private final RestClient restClient;
	private final String apiKey;

	@Autowired
	public RetellClient(QaaiProperties properties, RestClient.Builder restClientBuilder) {
		this(
				restClientBuilder.baseUrl(properties.retell().baseUrl()).build(),
				properties.retell().apiKey()
		);
	}

	public RetellClient(RestClient restClient, String apiKey) {
		this.restClient = restClient;
		this.apiKey = apiKey;
	}

	public RetellOutboundCallResponse createPhoneCall(RetellOutboundCallRequest request) {
		if (isBlank(apiKey)) {
			throw new RetellApiException("RETELL_API_KEY is required for --run-mode=retell");
		}

		try {
			return restClient.post()
					.uri("/v2/create-phone-call")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.body(request)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
						String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
						throw new RetellApiException(
								"Retell create-phone-call failed with HTTP "
										+ response.getStatusCode().value()
										+ ": "
										+ responseBody
						);
					})
					.body(RetellOutboundCallResponse.class);
		} catch (RetellApiException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new RetellApiException("Unable to create Retell phone call", exception);
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
			return restClient.get()
					.uri("/v2/get-call/{call_id}", retellCallId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
						String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
						throw new RetellApiException(
								"Retell get-call failed with HTTP "
										+ response.getStatusCode().value()
										+ ": "
										+ responseBody
						);
					})
					.body(RetellCallDetailsResponse.class);
		} catch (RetellApiException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new RetellApiException("Unable to retrieve Retell call details", exception);
		}
	}

	public byte[] downloadRecording(String recordingUrl) {
		if (isBlank(recordingUrl)) {
			throw new RetellApiException("recording_url is required to download audio");
		}

		try {
			return restClient.get()
					.uri(URI.create(recordingUrl))
					.retrieve()
					.onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
						String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
						throw new RetellApiException(
								"Recording download failed with HTTP "
										+ response.getStatusCode().value()
										+ ": "
										+ responseBody
						);
					})
					.body(byte[].class);
		} catch (RetellApiException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new RetellApiException("Unable to download Retell recording", exception);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
