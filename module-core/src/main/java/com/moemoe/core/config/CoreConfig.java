package com.moemoe.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@ComponentScan(basePackages = {"com.moemoe.core", "com.moemoe.client"})
@EnableRedisRepositories(basePackages = "com.moemoe.redis")
@EnableMongoRepositories(basePackages = "com.moemoe.mongo")
public class CoreConfig {
}
