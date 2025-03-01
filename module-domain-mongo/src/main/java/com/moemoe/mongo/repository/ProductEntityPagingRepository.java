package com.moemoe.mongo.repository;

import com.moemoe.mongo.entity.ProductEntity;

import java.util.List;

public interface ProductEntityPagingRepository {
    List<ProductEntity> findAll(String nextId, int pageSize);
}
