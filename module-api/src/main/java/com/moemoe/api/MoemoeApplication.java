package com.moemoe.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.moemoe.core", "com.moemoe.api"})
public class MoemoeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoemoeApplication.class, args);
    }

}
