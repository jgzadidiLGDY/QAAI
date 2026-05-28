package com.qaai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qaai")
public record QaaiProperties(
		Retell retell,
		Analysis analysis,
		Evaluation evaluation,
		OpenAi openai,
		Target target,
		Outputs outputs
) {

	public QaaiProperties(Retell retell, Analysis analysis, OpenAi openai, Target target, Outputs outputs) {
		this(retell, analysis, null, openai, target, outputs);
	}

	public QaaiProperties {
		if (retell == null) {
			retell = new Retell(null, null, null, "https://api.retellai.com", Duration.ofSeconds(30),
					Duration.ofSeconds(60));
		}
		if (analysis == null) {
			analysis = new Analysis("openai");
		}
		if (evaluation == null) {
			evaluation = new Evaluation("local");
		}
		if (openai == null) {
			openai = new OpenAi(null, "gpt-4.1-mini", Duration.ofSeconds(60));
		}
		if (target == null) {
			target = new Target("+18054398008");
		}
		if (outputs == null) {
			outputs = new Outputs("outputs");
		}
	}

	public record Analysis(String provider) {

		public Analysis {
			if (provider == null || provider.isBlank()) {
				provider = "openai";
			}
		}
	}

	public record Evaluation(String provider) {

		public Evaluation {
			if (provider == null || provider.isBlank()) {
				provider = "local";
			}
		}
	}

	public record Retell(String apiKey, String agentId, String fromNumber, String baseUrl, Duration apiTimeout,
			Duration recordingDownloadTimeout) {

		public Retell {
			if (apiTimeout == null) {
				apiTimeout = Duration.ofSeconds(30);
			}
			if (recordingDownloadTimeout == null) {
				recordingDownloadTimeout = Duration.ofSeconds(60);
			}
		}
	}

	public record OpenAi(String apiKey, String analysisModel, Duration analysisTimeout) {

		public OpenAi {
			if (analysisTimeout == null) {
				analysisTimeout = Duration.ofSeconds(60);
			}
		}
	}

	public record Target(String agentPhoneNumber) {
	}

	public record Outputs(String baseDir) {
	}
}
