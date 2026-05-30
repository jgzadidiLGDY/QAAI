package com.qaai.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgentProfileValidator {

	public void validate(AgentProfile profile) {
		List<String> errors = new ArrayList<>();
		if (profile == null) {
			throw new AgentProfileValidationException(List.of("agent profile is required"));
		}

		requireText(errors, "id", profile.id());
		requireText(errors, "name", profile.name());
		requireText(errors, "domain", profile.domain());
		requireText(errors, "description", profile.description());
		requireNonEmpty(errors, "supported_workflows", profile.supportedWorkflows());
		validateChannels(errors, profile.channels());

		if (!errors.isEmpty()) {
			throw new AgentProfileValidationException(errors);
		}
	}

	private void validateChannels(List<String> errors, Map<String, AgentProfile.ChannelProfile> channels) {
		if (channels == null || channels.isEmpty()) {
			errors.add("channels must include at least one channel");
			return;
		}
		for (Map.Entry<String, AgentProfile.ChannelProfile> entry : channels.entrySet()) {
			if (entry.getKey() == null || entry.getKey().isBlank()) {
				errors.add("channels key is required");
			}
			AgentProfile.ChannelProfile channel = entry.getValue();
			if (channel == null) {
				errors.add("channels." + entry.getKey() + " is required");
				continue;
			}
			requireText(errors, "channels." + entry.getKey() + ".provider", channel.provider());
		}
	}

	private static void requireText(List<String> errors, String field, String value) {
		if (value == null || value.isBlank()) {
			errors.add(field + " is required");
		}
	}

	private static void requireNonEmpty(List<String> errors, String field, List<String> values) {
		if (values == null || values.isEmpty()) {
			errors.add(field + " must include at least one item");
		}
	}
}
