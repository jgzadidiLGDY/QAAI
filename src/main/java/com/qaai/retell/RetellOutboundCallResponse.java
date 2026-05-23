package com.qaai.retell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RetellOutboundCallResponse(
		@JsonProperty("call_id")
		String callId,
		@JsonProperty("call_status")
		String callStatus,
		@JsonProperty("agent_id")
		String agentId,
		@JsonProperty("from_number")
		String fromNumber,
		@JsonProperty("to_number")
		String toNumber,
		String direction,
		Map<String, String> metadata
) {
}
