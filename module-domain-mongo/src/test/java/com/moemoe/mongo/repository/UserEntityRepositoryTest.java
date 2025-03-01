package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.UserEntity;
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
        UserEntity userEntity = UserEntity.builder()
                .name("test")
                .email("testemail@naver.com")
                .build();

        // when
        userEntityRepository.save(userEntity);

        // then
        Optional<UserEntity> byEmail = userEntityRepository.findByEmail("testemail@naver.com");
        assertThat(byEmail)
                .isNotEmpty();
    }

}