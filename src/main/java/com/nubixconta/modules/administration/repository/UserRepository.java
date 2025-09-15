package com.nubixconta.modules.administration.repository;



import com.nubixconta.modules.administration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(String email);

    boolean existsByUserName(String userName);
    Optional<User> findByUserName(String userName);
    List<User> findByRoleAndStatus(boolean role, boolean status);
    boolean existsByFirstNameAndLastName(String firstName, String lastName);
    long countByRole(boolean role);
}
