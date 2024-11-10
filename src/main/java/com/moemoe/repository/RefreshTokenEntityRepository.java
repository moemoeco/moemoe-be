package com.moemoe.repository;

import com.moemoe.domain.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenEntityRepository extends CrudRepository<RefreshToken, String> {
}
