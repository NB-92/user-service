package com.prpo.chat.service.repository;

import com.prpo.chat.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByWalletAddress(String walletAddress);

  Optional<User> findByUsername(String username);

  Optional<User> findByWalletAddress(String walletAddress);
}
