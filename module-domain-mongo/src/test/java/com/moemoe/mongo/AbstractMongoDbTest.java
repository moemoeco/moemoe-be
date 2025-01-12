package com.moemoe.mongo;

import com.moemoe.mongo.config.MongoConfig;
import com.moemoe.mongo.config.MongoTestConfig;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

@DataMongoTest
@ContextConfiguration(classes = {MongoConfig.class, MongoTestConfig.class})
public abstract class AbstractMongoDbTest {
}
