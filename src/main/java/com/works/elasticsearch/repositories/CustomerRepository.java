package com.works.elasticsearch.repositories;

import com.works.elasticsearch.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CustomerRepository extends JpaRepository<Customer,Integer> {

    void deleteCustomerByCid(Integer cid);

    Optional<Customer> findCustomerByCid(Integer cid);
}
