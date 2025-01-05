package com.moemoe.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.moemoe.core", "com.moemoe.api", "com.moemoe.mongo"})
@EnableMongoRepositories(basePackages = "com.moemoe.mongo")
public class MoemoeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoemoeApplication.class, args);
    }

}
