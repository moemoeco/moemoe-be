package com.moemoe.config;

import com.moemoe.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@Configuration
@RequiredArgsConstructor
public class WebConfig {
    private final UserEntityRepository userEntityRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return ((username) -> userEntityRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user name")));
    }

}
