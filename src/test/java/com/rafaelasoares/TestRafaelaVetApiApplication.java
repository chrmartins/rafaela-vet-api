package com.rafaelasoares;

import org.springframework.boot.SpringApplication;

public class TestRafaelaVetApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(RafaelaVetApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
