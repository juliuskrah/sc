package org.sc.ai.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(args = {"--base-url=http://localhost:11432"})
class TopCommandIT {
	
	private void bindAndAssert(Environment env, String expectedBaseUrl) {
		BindResult<OllamaConnectionProperties> bindResult = Binder.get(env).bind("spring.ai.ollama",
				OllamaConnectionProperties.class);
		bindResult.ifBound(properties -> {
			assertThat(properties.getBaseUrl()).isEqualTo(expectedBaseUrl);
		});
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void shouldLoadOllamaEndpointFromConfig(@Autowired ConfigurableEnvironment env) {
		// Ollama endpoint can be loaded from in order of priority:
		// 1. Command Line option: `--base-url`
		// 2. Config file located at /.sc/config: `ollama.base-url`
		// 3. Default value: `http://localhost:11434`
		env.getPropertySources().remove(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME);
		bindAndAssert(env, "http://localhost:11433");
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void shouldLoadDefaultOllamaEndpoint(@Autowired ConfigurableEnvironment env) {
		// Ollama endpoint can be loaded from in order of priority:
		// 1. Command Line option: `--base-url`
		// 2. Config file located at /.sc/config: `ollama.base-url`
		// 3. Default value: `http://localhost:11434`
		var userDirConfig = "Config resource 'file [%s/.sc/config]' via location 'optional:%s/.sc/config[.yaml]'"
				.formatted(System.getProperty("user.dir"), System.getProperty("user.dir"));
		var userHomeConfig = "Config resource 'file [%s/.sc/config]' via location 'optional:file:%s/.sc/config[.yaml]'"
				.formatted(System.getProperty("user.home"), System.getProperty("user.home"));
		// Remove the config file property sources to test default value
		env.getPropertySources().remove(userDirConfig);
		env.getPropertySources().remove(userHomeConfig);
		// Remove the command line property source to test default value
		env.getPropertySources().remove(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME);
		bindAndAssert(env, "http://localhost:11434");
	}

	@Test
	void shouldLoadOllamaEndpointFromCommandLine(@Autowired OllamaConnectionProperties properties) {
		// Ollama endpoint can be loaded from in order of priority:
		// 1. Command Line option: `--base-url`
		// 2. Config file located at /.sc/config: `ollama.base-url`
		// 3. Default value: `http://localhost:11434`
		assertThat(properties.getBaseUrl()).isEqualTo("http://localhost:11432");
	}

}
