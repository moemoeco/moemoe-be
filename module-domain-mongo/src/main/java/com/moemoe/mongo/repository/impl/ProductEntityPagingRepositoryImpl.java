package com.moemoe.mongo.repository.impl;

import com.moemoe.mongo.entity.Product;
import com.moemoe.mongo.repository.ProductEntityPagingRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductEntityPagingRepositoryImpl implements ProductEntityPagingRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Product> findAll(String nextId, int pageSize) {
        Query query = new Query();

        if (!ObjectUtils.isEmpty(nextId)) {
            ObjectId nextObjectId = new ObjectId(nextId);
            query.addCriteria(Criteria.where("_id").lt(nextObjectId));
        }
        query
                .limit(pageSize + 1)
                .with(Sort.by(Sort.Direction.DESC, "_id"));
        return mongoTemplate.find(query, Product.class);
    }
}