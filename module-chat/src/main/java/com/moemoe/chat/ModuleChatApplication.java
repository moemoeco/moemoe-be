package com.moemoe.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.moemoe.chat", "com.moemoe.mongo", "com.moemoe.redis"})
public class ModuleChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleChatApplication.class, args);
    }

}
