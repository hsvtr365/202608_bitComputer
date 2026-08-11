package com.bitcomputer.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmployeePortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeePortalApplication.class, args);
    }
}
