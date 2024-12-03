package com.moemoe.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@ComponentScan(basePackages = {"com.moemoe.core", "com.moemoe.client"})
@EnableRedisRepositories(basePackages = "com.moemoe.redis")
public class CoreConfig {
}
