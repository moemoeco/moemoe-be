package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


class UserEntityRepositoryTest extends AbstractMongoDbTest {
    @Autowired
    UserEntityRepository userEntityRepository;

    @AfterEach
    void deleteAll() {
        userEntityRepository.deleteAll();
    }

    @Test
    void test() {
        // given
        User user = User.builder()
                .name("test")
                .email("testemail@naver.com")
                .build();

        // when
        userEntityRepository.save(user);

        // then
        Optional<User> byEmail = userEntityRepository.findByEmail("testemail@naver.com");
        assertThat(byEmail)
                .isNotEmpty();
    }

}