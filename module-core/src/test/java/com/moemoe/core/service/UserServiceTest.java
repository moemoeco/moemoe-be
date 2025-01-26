package com.moemoe.core.service;

import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.jwt.ClaimsFactory;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.entity.RefreshToken;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @InjectMocks
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private RefreshTokenEntityRepository refreshTokenEntityRepository;

    @Test
    @DisplayName("성공 케이스 : Access Token 재발행")
    void refresh() {
        // given
        String expectedRefreshToken = "refreshToken";
        String expectedAccessToken = "accessToken";
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

        Map<String, String> userClaims = ClaimsFactory.getUserClaims(userEntity);
        given(jwtService.createAccessToken(userClaims, userEntity))
                .willReturn(expectedAccessToken);

        // when
        LoginTokenResponse loginTokenResponse = userService.refresh(expectedRefreshToken);

        // then
        assertThat(loginTokenResponse)
                .extracting(LoginTokenResponse::accessToken, LoginTokenResponse::refreshToken)
                .containsExactly(expectedAccessToken, expectedRefreshToken);
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(1))
                .findByEmail(expectedEmail);
        verify(jwtService, times(1))
                .createAccessToken(userClaims, userEntity);
    }

    @Test
    @DisplayName("실패 케이스 : Refresh Token Entity 데이터가 없는 경우")
    void refreshNoRefreshTokenEntity() {
        // given
        String expectedRefreshToken = "refreshToken";
        given(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .willReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> userService.refresh(expectedRefreshToken))
                .isInstanceOf(NoSuchElementException.class);

        // then
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(0))
                .findByEmail(any());
        verify(jwtService, times(0))
                .createAccessToken(any(), any());
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
        assertThatThrownBy(() -> userService.refresh(expectedRefreshToken))
                .isInstanceOf(NoSuchElementException.class);

        // then
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(1))
                .findByEmail(expectedEmail);
        verify(jwtService, times(0))
                .createAccessToken(any(), any());
    }
}