package com.sysdev.slat.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

// CrudRepositoryを継承: <エンティティ, IDの型>
public interface UserRepository extends CrudRepository<User, UUID> {
  Optional<User> findByUsername(String username);
}
