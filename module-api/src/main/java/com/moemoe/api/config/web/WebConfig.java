package com.moemoe.api.config.web;


import com.moemoe.api.config.filter.RequestLoggingFilter;
import com.moemoe.api.constant.OAuthPlatformConverter;
import com.moemoe.mongo.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final UserEntityRepository userEntityRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return (userName -> userEntityRepository.findByEmail(userName)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user name")));
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new OAuthPlatformConverter());
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilter() {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestLoggingFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
