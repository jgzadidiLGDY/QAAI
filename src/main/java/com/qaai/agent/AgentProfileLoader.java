package com.qaai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class AgentProfileLoader {

	private final ObjectMapper yamlMapper;

	public AgentProfileLoader() {
		this(new ObjectMapper(new YAMLFactory()).findAndRegisterModules());
	}

	AgentProfileLoader(ObjectMapper yamlMapper) {
		this.yamlMapper = yamlMapper;
	}

	public AgentProfile load(Path profilePath) {
		try {
			return yamlMapper.readValue(profilePath.toFile(), AgentProfile.class);
		} catch (IOException exception) {
			throw new AgentProfileLoadException("Unable to load agent profile YAML: " + profilePath, exception);
		}
	}
}
