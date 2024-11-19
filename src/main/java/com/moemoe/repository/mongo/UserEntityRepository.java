package com.moemoe.repository.mongo;

import com.moemoe.domain.mongo.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserEntityRepository extends MongoRepository<User, String> {
    @Query("{ 'email' : ?0 }")
    Optional<User> findByEmail(String email);
}
