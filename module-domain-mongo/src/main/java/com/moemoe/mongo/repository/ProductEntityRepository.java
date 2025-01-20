package com.moemoe.mongo.repository;

import com.moemoe.mongo.entity.Product;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductEntityRepository extends MongoRepository<Product, ObjectId>, ProductEntityPagingRepository {
}
