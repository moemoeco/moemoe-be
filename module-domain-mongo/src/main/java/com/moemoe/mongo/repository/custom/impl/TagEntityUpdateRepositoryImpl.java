package com.moemoe.mongo.repository.custom.impl;

import com.moemoe.mongo.entity.TagEntity;
import com.moemoe.mongo.repository.custom.TagEntityUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import static com.moemoe.mongo.utils.MongoUpdateQueryUtil.withUpdatedAt;

@Repository
@RequiredArgsConstructor
public class TagEntityUpdateRepositoryImpl implements TagEntityUpdateRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public void incrementProductsCount(String name) {
        Query query = Query.query(Criteria.where("name").is(name));
        Update update = new Update()
                .inc("productsCount", 1);
        mongoTemplate.updateFirst(query, withUpdatedAt(update), TagEntity.class);
    }

    @Override
    public void decrementProductsCount(String name) {
        Query query = Query.query(Criteria.where("name").is(name));
        Update update = new Update()
                .inc("productsCount", -1);
        mongoTemplate.updateFirst(query, withUpdatedAt(update), TagEntity.class);
    }
}
