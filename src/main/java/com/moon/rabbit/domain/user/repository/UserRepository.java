package com.moon.rabbit.domain.user.repository;
import com.moon.rabbit.domain.user.entity.User;
import com.moon.rabbit.global.security.jwt.dto.UserCredential;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, Long> {
    Optional<UserCredential> findCredentialById(Long userId);
    Optional<User> findByGoogleId(String googleId);
}
