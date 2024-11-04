package com.moemoe.repository;

import com.moemoe.domain.User;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataMongoTest
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
        User user = User.builder().name("test").build();

        // when
        userEntityRepository.save(user);

        // then
        List<User> all = userEntityRepository.findAll();
        assertThat(all)
                .isNotEmpty()
                .extracting(User::getId, User::getName)
                .containsExactly(Tuple.tuple(user.getId(), "test"));
    }

}