package com.nubixconta.modules.sales.service;

import com.nubixconta.modules.sales.entity.Customer;
import com.nubixconta.modules.sales.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }
    public Customer findById(Integer id) {
        return customerRepository.findById(id).orElse(null);
    }
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }
    public void delete(Integer id) {
        customerRepository.deleteById(id);
    }
}