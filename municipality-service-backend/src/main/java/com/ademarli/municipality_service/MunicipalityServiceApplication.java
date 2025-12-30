package com.ademarli.municipality_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ademarli.municipality_service")
public class MunicipalityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MunicipalityServiceApplication.class, args);
    }

}
