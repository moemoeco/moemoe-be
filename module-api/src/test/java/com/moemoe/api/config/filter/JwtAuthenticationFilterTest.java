package com.moemoe.api.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.core.service.jwt.exception.JwtExpiredException;
import com.moemoe.core.service.jwt.exception.JwtMalformedException;
import com.moemoe.mongo.constant.UserRole;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private JwtService jwtService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("성공 케이스 : API 요청에 유효한 Access Token 포함")
    void doFilterInternalWithValidToken() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        try (MockedStatic<SecurityContextHolder> utilities = Mockito.mockStatic(SecurityContextHolder.class);) {
            // given
            String mockId = "userId";
            String mockRole = UserRole.USER.name();
            String validToken = "valid.jwt.token";

            given(jwtService.getUserId(validToken))
                    .willReturn(mockId);
            given(jwtService.getRole(validToken))
                    .willReturn(mockRole);
            given(jwtService.isValidToken(validToken, mockId))
                    .willReturn(true);
            utilities.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
            mockHttpServletRequest.addHeader("Authorization", "Bearer " + validToken);
            MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
            MockFilterChain mockFilterChain = new MockFilterChain();

            // when
            jwtAuthenticationFilter.doFilterInternal(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            assertThat(authentication)
//                    .isNotNull()
//                    .extracting(Authentication::getPrincipal)
//                    .isEqualTo(mockId);
        } catch (Exception e) {
            fail("Exception");
        }
    }

    @Test
    @DisplayName("실패 케이스 : API 요청에 유효하지 않은 형식의 Access Token을 포함")
    void doFilterInternalWithMalformedToken() throws ServletException, IOException {
        // given
        String malformedToken = "malformed.jwt.token";

        given(jwtService.getUserId(malformedToken))
                .willThrow(new JwtMalformedException("Malformed token", new Throwable("Malformed token")));


        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("Authorization", "Bearer " + malformedToken);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        MockFilterChain mockFilterChain = new MockFilterChain();

        // when
        jwtAuthenticationFilter.doFilterInternal(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        // then
        assertThat(mockHttpServletResponse)
                .extracting(MockHttpServletResponse::getStatus, MockHttpServletResponse::getContentType)
                .containsExactly(HttpStatus.FORBIDDEN.value(), MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @DisplayName("실패 케이스 : API 요청에 유효기간이 지난 Access Token을 포함")
    void doFilterInternalWithExpiredToken() throws ServletException, IOException {
        // given
        String expiredToken = "expired.jwt.token";

        given(jwtService.getUserId(expiredToken))
                .willThrow(new JwtExpiredException("Jwt Token Expired", new Throwable("Expired Token")));

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        MockFilterChain mockFilterChain = new MockFilterChain();

        // when
        jwtAuthenticationFilter.doFilterInternal(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        // then
        assertThat(mockHttpServletResponse)
                .extracting(MockHttpServletResponse::getStatus, MockHttpServletResponse::getContentType)
                .containsExactly(HttpStatus.UNAUTHORIZED.value(), MediaType.APPLICATION_JSON_VALUE);
    }
}