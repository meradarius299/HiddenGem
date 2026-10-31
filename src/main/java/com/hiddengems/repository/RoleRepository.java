package com.hiddengems.repository;

import com.hiddengems.entity.Role;
import com.hiddengems.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByName(RoleName name);
}
