package com.qaai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QAAIApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(QAAIApplication.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}
}
