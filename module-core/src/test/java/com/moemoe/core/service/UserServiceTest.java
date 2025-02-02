package com.moemoe.core.service;

import com.moemoe.core.request.RefreshAccessTokenRequest;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.entity.RefreshToken;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @InjectMocks
    private UserService userService;
    @Spy
    private JwtService jwtService = new JwtService(TEST_SECRET_KEY);
    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private RefreshTokenEntityRepository refreshTokenEntityRepository;
    private static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5LTI1Ni1iaXRzLWZvci1qd3QtdGVzdGluZw==";


    @Test
    @DisplayName("성공 케이스 : Access Token 재발행")
    void refresh() {
        // given
        String expectedRefreshToken = "refreshToken";
        String expectedEmail = "user@moemoe.com";
        RefreshToken refreshTokenEntity = RefreshToken.of(expectedEmail, expectedRefreshToken);
        User userEntity = User.builder()
                .email(expectedEmail)
                .role(UserRole.USER)
                .build();
        given(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .willReturn(Optional.of(refreshTokenEntity));
        given(userEntityRepository.findByEmail(expectedEmail))
                .willReturn(Optional.of(userEntity));

        // when
        RefreshAccessTokenRequest refreshAccessTokenRequest = new RefreshAccessTokenRequest();
        ReflectionTestUtils.setField(refreshAccessTokenRequest, "refreshToken", expectedRefreshToken);
        LoginTokenResponse loginTokenResponse = userService.refresh(refreshAccessTokenRequest);

        // then
        assertThat(loginTokenResponse)
                .extracting(LoginTokenResponse::refreshToken)
                .isEqualTo(expectedRefreshToken);
        assertDoesNotThrow(() -> Jwts.parser()
                .clockSkewSeconds(Integer.MAX_VALUE) // 만료 검사 무시
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(TEST_SECRET_KEY)))
                .build()
                .parse(loginTokenResponse.accessToken())
                .getPayload());
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(1))
                .findByEmail(expectedEmail);
    }

    @Test
    @DisplayName("실패 케이스 : Refresh Token Entity 데이터가 없는 경우")
    void refreshNoRefreshTokenEntity() {
        // given
        String expectedRefreshToken = "refreshToken";
        given(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .willReturn(Optional.empty());

        // when
        RefreshAccessTokenRequest refreshAccessTokenRequest = new RefreshAccessTokenRequest();
        ReflectionTestUtils.setField(refreshAccessTokenRequest, "refreshToken", expectedRefreshToken);
        assertThatThrownBy(() -> userService.refresh(refreshAccessTokenRequest))
                .isInstanceOf(NoSuchElementException.class);

        // then
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(0))
                .findByEmail(any());
    }

    @Test
    @DisplayName("실패 케이스 : User Entity 데이터가 없는 경우")
    void refreshNoUserEntity() {
        // given
        String expectedRefreshToken = "refreshToken";
        String expectedEmail = "user@moemoe.com";
        RefreshToken refreshTokenEntity = RefreshToken.of(expectedEmail, expectedRefreshToken);
        given(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .willReturn(Optional.of(refreshTokenEntity));
        given(userEntityRepository.findByEmail(expectedEmail))
                .willReturn(Optional.empty());

        // when
        RefreshAccessTokenRequest refreshAccessTokenRequest = new RefreshAccessTokenRequest();
        ReflectionTestUtils.setField(refreshAccessTokenRequest, "refreshToken", expectedRefreshToken);
        assertThatThrownBy(() -> userService.refresh(refreshAccessTokenRequest))
                .isInstanceOf(NoSuchElementException.class);

        // then
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(1))
                .findByEmail(expectedEmail);
    }
}