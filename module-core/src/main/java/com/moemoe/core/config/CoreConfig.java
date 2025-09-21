package com.moemoe.core.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@ComponentScan(basePackages = {"com.moemoe.core", "com.moemoe.client"})
@EnableRedisRepositories(basePackages = "com.moemoe.redis", enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
@EnableMongoRepositories(basePackages = "com.moemoe.mongo")
@ConfigurationPropertiesScan(basePackages = "com.moemoe.core.property")
public class CoreConfig {
}
