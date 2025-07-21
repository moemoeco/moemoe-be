package com.moemoe.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moemoe.api.config.web.SecurityConfig;
import com.moemoe.core.service.UserService;
import com.moemoe.core.service.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.UnsupportedEncodingException;

import static com.moemoe.core.service.jwt.JwtService.AUTHENTICATION_HEADER;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@Slf4j
@Disabled
@AutoConfigureMockMvc
@WithMockUser(username = "user@moemoe.com", roles = "USER")
@Import(SecurityConfig.class)
public abstract class AbstractControllerTest {
    @MockBean
    protected JwtService jwtService;
    @MockBean
    protected UserService userService;
    @Autowired
    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected MvcResult invoke(MockHttpServletRequestBuilder builder, ResultMatcher statusMatcher, boolean hasAuth) {
        return invoke(builder, statusMatcher, null, hasAuth);
    }

    protected MvcResult invoke(MockHttpServletRequestBuilder builder, ResultMatcher statusMatcher, String expectedJson, boolean hasAuth) {
        if (hasAuth) {
            builder = attachAuthHeader(builder);
        }

        try {
            ResultActions resultActions = mockMvc.perform(builder)
                    .andExpect(statusMatcher);

            if (expectedJson != null) {
                resultActions.andExpect(content().json(expectedJson));
            }

            resultActions.andDo(print());
            return resultActions.andReturn();

        } catch (Exception e) {
            fail("API invocation failed.", e);
            throw new IllegalStateException("API invocation failed.");
        }
    }

    private MockHttpServletRequestBuilder attachAuthHeader(MockHttpServletRequestBuilder builder) {
        String accessToken = "expectedAccessToken";
        String userId = "userId";

        given(jwtService.getUserId(accessToken)).willReturn(userId);
        given(jwtService.getEmail(accessToken)).willReturn("user@moemoe.com");
        given(jwtService.getRole(accessToken)).willReturn("USER");
        given(jwtService.isValidToken(accessToken, userId)).willReturn(true);

        return builder.header(AUTHENTICATION_HEADER, "Bearer " + accessToken);
    }


    protected String convertObjectToJson(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T convertResponseToClass(MvcResult mvcResult, Class<T> type) {
        try {
            String content = mvcResult.getResponse().getContentAsString();
            return objectMapper.readValue(content, type);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
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
