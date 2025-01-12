package com.moemoe.mongo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@TestConfiguration
@EnableMongoRepositories(basePackages = "com.moemoe.mongo")
@ComponentScan(basePackages = "com.moemoe.mongo")
public class MongoTestConfig {
}
