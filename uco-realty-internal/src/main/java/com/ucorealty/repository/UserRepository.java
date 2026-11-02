package com.ucorealty.repository;

import com.ucorealty.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByUsername(String username);
}
