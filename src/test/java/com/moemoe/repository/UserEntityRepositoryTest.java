package com.moemoe.repository;

import com.moemoe.domain.mongo.User;
import com.moemoe.repository.mongo.UserEntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@ActiveProfiles("test")
class UserEntityRepositoryTest {
    @Autowired
    UserEntityRepository userEntityRepository;

    @AfterEach
    void deleteAll(){
        userEntityRepository.deleteAll();
    }

    @Test
    void test() {
        // given
        User user = User.builder().name("test").email("testemail@naver.com").build();

        // when
        userEntityRepository.save(user);

        // then
        Optional<User> byEmail = userEntityRepository.findByEmail("testemail@naver.com");
        assertThat(byEmail)
                .isNotEmpty();
    }

}