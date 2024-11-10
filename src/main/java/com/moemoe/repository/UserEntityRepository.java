package com.moemoe.repository;

import com.moemoe.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserEntityRepository extends MongoRepository<User, String> {
    @Query("{ 'email' : ?0 }")
    Optional<User> findByEmail(String email);
}
