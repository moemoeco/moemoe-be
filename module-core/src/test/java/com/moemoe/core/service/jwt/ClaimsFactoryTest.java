package com.moemoe.core.service.jwt;

import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimsFactoryTest {

    @Test
    @DisplayName("성공 케이스 : User Claims 생성")
    void getUserClaims() {
        // given
        User userEntity = User.builder()
                .email("user@moemoe.com")
                .role(UserRole.USER)
                .build();

        // when
        Map<String, String> userClaims = ClaimsFactory.getUserClaims(userEntity);

        // then
        assertThat(userClaims)
                .hasSize(2)
                .containsExactlyEntriesOf(
                        Map.of("email", userEntity.getEmail(), "role", userEntity.getRole().name())
                );

    }
}