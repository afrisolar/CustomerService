package com.afrisol.CustomerService.repository;

import com.afrisol.CustomerService.model.Customer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository  extends ReactiveCrudRepository<Customer,String> {
    Mono<Boolean> existsByEmail(String email);
    Mono<Customer> findByEmail(String email);
    Mono<Customer> findByPhone( String phone);
    Mono<Customer> findByCustomerId(String customerId);
}
