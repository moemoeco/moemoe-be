package com.moemoe.mongo.repository;


import com.moemoe.mongo.entity.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface TagEntityRepository extends MongoRepository<Tag, String> {
    @Query("{ 'name': ?0 }")
    @Update("{ '$inc': { 'productsCount': 1 } }")
    void incrementProductsCount(String name);

    @Query("{ 'name': ?0 }")
    @Update("{ '$inc': { 'productsCount': -1 } }")
    void decrementProductsCount(String name);

    List<Tag> findTop20ByNameStartingWith(String prefix, Sort sort);
}
