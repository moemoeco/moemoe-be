package com.moemoe.core.security;

import com.moemoe.mongo.constant.UserRole;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityContextHolderUtilsTest {

    @Test
    @DisplayName("성공 케이스 : SecurityContext에 존재하는 userId 조회")
    void getUserId() {
        // given
        String id = new ObjectId().toHexString();
        String email = "moemoe@example.com";
        UserRole userRole = UserRole.USER;

        MoeUser moeUser = MoeUser.of(id, email, userRole);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                moeUser,
                null,
                null
        );
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

        // when
        ObjectId userId = SecurityContextHolderUtils.getUserId();

        // then
        assertThat(userId)
                .isEqualTo(new ObjectId(id));
    }
}