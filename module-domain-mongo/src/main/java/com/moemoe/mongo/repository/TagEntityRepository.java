package com.moemoe.mongo.repository;


import com.moemoe.mongo.entity.TagEntity;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagEntityRepository extends MongoRepository<TagEntity, ObjectId> {
    Optional<TagEntity> findTagEntityByName(String name);

    List<TagEntity> findAllByNameIn(Collection<String> names);

    @Query("{ 'name': ?0 }")
    @Update("{ '$inc': { 'productsCount': 1 } }")
    void incrementProductsCount(String name);

    @Query("{ 'name': ?0 }")
    @Update("{ '$inc': { 'productsCount': -1 } }")
    void decrementProductsCount(String name);

    List<TagEntity> findTop20ByNameStartingWith(String prefix, Sort sort);
}
