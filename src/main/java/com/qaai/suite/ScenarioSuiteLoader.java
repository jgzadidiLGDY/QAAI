package com.qaai.suite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class ScenarioSuiteLoader {

	private final ObjectMapper yamlMapper;

	public ScenarioSuiteLoader() {
		this(new ObjectMapper(new YAMLFactory()).findAndRegisterModules());
	}

	ScenarioSuiteLoader(ObjectMapper yamlMapper) {
		this.yamlMapper = yamlMapper;
	}

	public ScenarioSuite load(Path suitePath) {
		try {
			return yamlMapper.readValue(suitePath.toFile(), ScenarioSuite.class);
		} catch (IOException exception) {
			throw new ScenarioSuiteLoadException("Unable to load scenario suite YAML: " + suitePath, exception);
		}
	}
}
