package com.nubixconta.modules.sales.repository;
import com.nubixconta.modules.sales.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CustomerRepository extends JpaRepository<Customer, Integer>{
}
