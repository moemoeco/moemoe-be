package com.moemoe.mongo.repository.custom;

public interface TagEntityUpdateRepository {
    void incrementProductsCount(String name);

    void decrementProductsCount(String name);
}
