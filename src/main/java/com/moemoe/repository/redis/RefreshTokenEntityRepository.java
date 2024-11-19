package com.moemoe.repository.redis;

import com.moemoe.domain.redis.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenEntityRepository extends CrudRepository<RefreshToken, String> {
}
