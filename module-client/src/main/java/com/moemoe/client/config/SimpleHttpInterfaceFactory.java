package com.moemoe.client.config;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * 프록시 객체 생성
 */
public class SimpleHttpInterfaceFactory {
    public <S> S create(Class<S> clientClass) {
        HttpExchange httpExchange = AnnotationUtils.getAnnotation(clientClass, HttpExchange.class);
        if (httpExchange == null) {
            throw new IllegalStateException("HttpExchange annotation not found");
        }

        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(WebClient.create(httpExchange.url())))
                .build()
                .createClient(clientClass);
    }
}
