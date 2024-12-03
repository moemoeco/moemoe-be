package com.moemoe.redis.repository;

import com.moemoe.redis.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenEntityRepository extends CrudRepository<RefreshToken, String> {
}
