package com.qaai.retell;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RetellOutboundCallRequest(
		@JsonProperty("from_number")
		String fromNumber,
		@JsonProperty("to_number")
		String toNumber,
		@JsonProperty("override_agent_id")
		String overrideAgentId,
		Map<String, String> metadata,
		@JsonProperty("retell_llm_dynamic_variables")
		Map<String, String> retellLlmDynamicVariables
) {
}
