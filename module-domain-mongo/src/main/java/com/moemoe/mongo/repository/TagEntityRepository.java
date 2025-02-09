package com.moemoe.mongo.repository;


import com.moemoe.mongo.entity.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TagEntityRepository extends MongoRepository<Tag, String> {
}
