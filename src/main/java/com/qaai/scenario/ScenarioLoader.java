package com.qaai.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class ScenarioLoader {

	private final ObjectMapper yamlMapper;

	public ScenarioLoader() {
		this(new ObjectMapper(new YAMLFactory()).findAndRegisterModules());
	}

	ScenarioLoader(ObjectMapper yamlMapper) {
		this.yamlMapper = yamlMapper;
	}

	public Scenario load(Path scenarioPath) {
		try {
			return yamlMapper.readValue(scenarioPath.toFile(), Scenario.class);
		} catch (IOException exception) {
			throw new ScenarioLoadException("Unable to load scenario YAML: " + scenarioPath, exception);
		}
	}
}
