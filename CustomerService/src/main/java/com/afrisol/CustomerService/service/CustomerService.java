package com.afrisol.CustomerService.service;

import com.afrisol.CustomerService.dto.CustomerRequestDto;
import com.afrisol.CustomerService.dto.CustomerResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerService {
    Mono<CustomerResponseDto> addCustomer(CustomerRequestDto customerDto, String requestID);
    Mono<CustomerResponseDto> getCustomer(String phoneNumber, String requestID);
    Flux<CustomerResponseDto> getAllCustomers(String requestID);
    Mono<CustomerResponseDto> updateCustomer(CustomerRequestDto customerDto, String customerId, String requestID);
    Mono<Void> deleteCustomer(String customerId, String requestID);
}
