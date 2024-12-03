package com.moemoe.client.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moemoe.client.config.HttpBeanFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = {HttpBeanFactory.class})
public abstract class AbstractMockWebServer {
    protected MockWebServer mockWebServer;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void destroy() throws IOException {
        mockWebServer.shutdown();
    }

    protected void mockResponse(Object expectedResponseObject, Map<String, Object> headers) {
        ObjectMapper objectMapper = new ObjectMapper();
        String responseJson = null;
        try {
            responseJson = objectMapper.writeValueAsString(expectedResponseObject);
        } catch (JsonProcessingException e) {
            fail("Json Parsing Failed.");
        }
        MockResponse mockResponse = new MockResponse()
                .setBody(responseJson)
                .setResponseCode(HttpStatus.OK.value());

        if (!ObjectUtils.isEmpty(headers)) {
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                mockResponse.addHeader(header.getKey(), header.getValue());
            }
        }
        mockWebServer
                .enqueue(mockResponse);
    }

    protected URI getUri(String expectedUrl) {
        return URI.create(mockWebServer.url(expectedUrl).toString());
    }
}
