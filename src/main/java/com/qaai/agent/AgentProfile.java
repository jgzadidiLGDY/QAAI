package com.qaai.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record AgentProfile(
		String id,
		String name,
		String domain,
		String description,
		@JsonProperty("supported_workflows")
		List<String> supportedWorkflows,
		Map<String, ChannelProfile> channels
) {
	public record ChannelProfile(
			String provider,
			@JsonProperty("target_phone_number")
			String targetPhoneNumber
	) {
	}
}
