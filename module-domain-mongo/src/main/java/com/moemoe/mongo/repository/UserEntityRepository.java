package com.moemoe.mongo.repository;

import com.moemoe.mongo.entity.UserEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UserEntityRepository extends MongoRepository<UserEntity, ObjectId> {
    @Query("{ 'email' : ?0 }")
    Optional<UserEntity> findByEmail(String email);
}
