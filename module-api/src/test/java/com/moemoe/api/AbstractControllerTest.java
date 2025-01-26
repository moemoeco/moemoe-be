package com.moemoe.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moemoe.core.service.UserService;
import com.moemoe.core.service.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Disabled
@AutoConfigureMockMvc
@WithMockUser
public abstract class AbstractControllerTest {
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserService userService;
    @Autowired
    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected Map<String, Object> getRequest(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be provided in pairs (key, value).");
        }

        Map<String, Object> requestMap = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Object value = keyValuePairs[i + 1];
            requestMap.put(key, value);
        }

        return requestMap;
    }

    protected String convertRequestToJson(Map<?, ?> request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String convertRequestToJson(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T convertResponseToClass(ResultActions resultActions, Class<T> type) {
        String content = getContent(resultActions);
        try {
            return objectMapper.readValue(content, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getContent(ResultActions resultActions) {
        String content = null;
        try {
            content = resultActions.andReturn()
                    .getResponse()
                    .getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
