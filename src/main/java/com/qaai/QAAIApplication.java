package com.qaai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QAAIApplication {

	public static void main(String[] args) {
		SpringApplication.run(QAAIApplication.class, args);
	}
}
