package com.qaai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qaai")
public record QaaiProperties(
		Retell retell,
		OpenAi openai,
		Target target,
		Outputs outputs
) {

	public QaaiProperties {
		if (retell == null) {
			retell = new Retell(null, null, null, "https://api.retellai.com");
		}
		if (openai == null) {
			openai = new OpenAi(null, "gpt-4.1-mini");
		}
		if (target == null) {
			target = new Target("+18054398008");
		}
		if (outputs == null) {
			outputs = new Outputs("outputs");
		}
	}

	public record Retell(String apiKey, String agentId, String fromNumber, String baseUrl) {
	}

	public record OpenAi(String apiKey, String analysisModel) {
	}

	public record Target(String agentPhoneNumber) {
	}

	public record Outputs(String baseDir) {
	}
}
