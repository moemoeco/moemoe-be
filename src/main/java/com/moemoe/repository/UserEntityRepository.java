package com.moemoe.repository;

import com.moemoe.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserEntityRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
}
