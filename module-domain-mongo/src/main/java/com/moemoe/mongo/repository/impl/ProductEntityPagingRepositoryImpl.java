package com.moemoe.mongo.repository.impl;

import com.moemoe.mongo.entity.ProductEntity;
import com.moemoe.mongo.repository.ProductEntityPagingRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductEntityPagingRepositoryImpl implements ProductEntityPagingRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<ProductEntity> findPage(String nextId, int pageSize) {
        Query query = new Query();

        if (StringUtils.hasText(nextId)) {
            String trimmed = nextId.trim();
            if (!ObjectId.isValid(trimmed)) {
                throw new IllegalArgumentException("Invalid ObjectId: " + nextId);
            }
            query.addCriteria(Criteria.where("_id").lt(new ObjectId(trimmed)));
        }

        query.fields()
                .include("_id")
                .include("title")
                .include("location.detailedAddress")
                .include("price")
                .include("imageKeys")
                .include("tagNames")
                .include("createdAt")
                .include("updatedAt");

        query.with(Sort.by(Sort.Direction.DESC, "_id"))
                .limit(pageSize + 1);

        return mongoTemplate.find(query, ProductEntity.class);
    }
}