package com.nubixconta.modules.accounting.repository;

import com.nubixconta.modules.accounting.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    List<Account> findByAccountType(String accountType);
    Optional<Account> findByAccountNameIgnoreCase(String name);
}
