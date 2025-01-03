package com.afrisol.CustomerService.service;

import com.afrisol.CustomerService.dto.CustomerRequest;
import com.afrisol.CustomerService.dto.CustomerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Mono<CustomerResponse> addCustomer(CustomerRequest customerRequest, String requestID);
    Mono<CustomerResponse> getCustomer(String phoneNumber, String requestID);
    Flux<CustomerResponse> getAllCustomers(String requestID);
    Mono<CustomerResponse> updateCustomer(CustomerRequest customerRequest, String customerId, String requestID);
    Mono<Void> deleteCustomer(String customerId, String requestID);
}
