package com.nubixconta.modules.administration.repository;


import com.nubixconta.modules.administration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
