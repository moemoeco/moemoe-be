package com.moemoe.mongo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@TestConfiguration
@EnableMongoRepositories(basePackages = "com.moemoe.mongo")
public class MongoTestConfig {
}
