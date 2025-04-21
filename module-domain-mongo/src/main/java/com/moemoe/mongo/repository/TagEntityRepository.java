package com.moemoe.mongo.repository;


import com.moemoe.mongo.entity.TagEntity;
import com.moemoe.mongo.repository.custom.TagEntityUpdateRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagEntityRepository extends MongoRepository<TagEntity, ObjectId>, TagEntityUpdateRepository {
    Optional<TagEntity> findTagEntityByName(String name);

    List<TagEntity> findAllByNameIn(Collection<String> names);

    List<TagEntity> findTop20ByNameStartingWith(String prefix, Sort sort);
}
