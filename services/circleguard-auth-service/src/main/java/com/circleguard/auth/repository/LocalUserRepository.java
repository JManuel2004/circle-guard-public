package com.circleguard.auth.repository;

import com.circleguard.auth.model.LocalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface LocalUserRepository extends JpaRepository<LocalUser, UUID> {
    Optional<LocalUser> findByUsername(String username);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM LocalUser u JOIN u.roles r JOIN r.permissions p WHERE p.name = :permissionName")
    java.util.List<LocalUser> findUsersByPermissionName(@org.springframework.data.repository.query.Param("permissionName") String permissionName);
}
