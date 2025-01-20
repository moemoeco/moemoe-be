package com.moemoe.mongo.repository;

import com.moemoe.mongo.entity.Product;

import java.util.List;

public interface ProductEntityPagingRepository {
    List<Product> findAll(String nextId, int pageSize);
}
