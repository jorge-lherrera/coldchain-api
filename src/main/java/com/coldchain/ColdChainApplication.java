package com.coldchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ColdChainApplication {

	public static void main(String[] args) {
		SpringApplication.run(ColdChainApplication.class, args);
	}

}
