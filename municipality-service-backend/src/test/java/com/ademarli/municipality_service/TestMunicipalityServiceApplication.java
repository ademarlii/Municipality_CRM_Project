package com.ademarli.municipality_service;

import org.springframework.boot.SpringApplication;

public class TestMunicipalityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(MunicipalityServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
