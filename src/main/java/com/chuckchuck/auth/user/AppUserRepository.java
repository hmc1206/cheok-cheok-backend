package com.chuckchuck.auth.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {
    Optional<AppUser> findByGoogleId(String googleId);
}
