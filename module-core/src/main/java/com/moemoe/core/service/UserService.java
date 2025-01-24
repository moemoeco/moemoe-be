package com.moemoe.core.service;

import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserEntityRepository userEntityRepository;

    public User getUser(String email) {
        return userEntityRepository.findByEmail(email)
                .orElseThrow();
    }
}
