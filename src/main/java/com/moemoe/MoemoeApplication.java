package com.moemoe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.moemoe.repository")
public class MoemoeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoemoeApplication.class, args);
    }

}
