package com.moemoe.mongo;

import com.moemoe.mongo.config.MongoConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;

@DataMongoTest
@ContextConfiguration(classes = {MongoConfig.class})
public abstract class AbstractMongoDbTest {
    @Autowired
    protected MongoTemplate mongoTemplate;
}
