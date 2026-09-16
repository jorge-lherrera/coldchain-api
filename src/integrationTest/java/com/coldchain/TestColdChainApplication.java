package com.coldchain;

import org.springframework.boot.SpringApplication;

public class TestColdChainApplication {

	public static void main(String[] args) {
		SpringApplication.from(ColdChainApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
