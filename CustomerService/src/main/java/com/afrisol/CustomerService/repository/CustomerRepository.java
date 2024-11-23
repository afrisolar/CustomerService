package com.afrisol.CustomerService.repository;

import com.afrisol.CustomerService.model.Customer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CustomerRepository  extends ReactiveCrudRepository<Customer,String> {
    Mono<Boolean> existsByEmail(String email);
    Mono<Customer> findByEmail(String email);
}
