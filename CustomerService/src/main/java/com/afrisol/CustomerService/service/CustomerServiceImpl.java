package com.afrisol.CustomerService.service;

import com.afrisol.CustomerService.dto.CustomerRequestDto;
import com.afrisol.CustomerService.dto.CustomerResponseDto;
import com.afrisol.CustomerService.exception.CustomException;
import com.afrisol.CustomerService.exception.CustomerAlreadyExistsException;
import com.afrisol.CustomerService.exception.CustomerNotFoundException;
import com.afrisol.CustomerService.model.Customer;
import com.afrisol.CustomerService.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Mono<CustomerResponseDto> addCustomer(CustomerRequestDto customerDto, String requestID) {
        if (customerDto == null) {
            return Mono.error(new IllegalArgumentException("CustomerRequestDto cannot be null"));
        }
        return customerRepository.existsByEmail (customerDto.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new CustomerAlreadyExistsException("Customer already exists"));
                    }
                    return  customerRepository.save(Customer.builder()
                            .address(customerDto.getAddress())
                            .email(customerDto.getEmail())
                            .phone(customerDto.getPhone())
                            .lastName(customerDto.getLastName())
                            .firstName(customerDto.getFirstName())
                            .dateOfBirth(customerDto.getDateOfBirth())
                            .income(customerDto.getIncome())
                            .build())
                      .doOnNext(savedCustomer ->
                            log.info("Successfully added customer with ID: {} with requestID {}", savedCustomer.getCustomerId(),requestID)
                    ).map(this::mapToCustomerResponseDto);
                });
    }

    @Override
    public Mono<CustomerResponseDto> getCustomer(String email, String requestID) {
        if (email == null || email.isEmpty()) {
            return Mono.error(new CustomException(HttpStatus.BAD_REQUEST,"Email cannot be null"));
        }
        log.info("Searching for customer with email: {}", email);
        email = email.toLowerCase();
        return customerRepository.findByEmail(email)
                .doOnNext(customer -> log.info("Successfully retrieved customer with ID: {} with requestID {}", customer.getCustomerId(), requestID))
                .map(this::mapToCustomerResponseDto)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found")))
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException) {
                        return Mono.error(e);
                    }
                    return Mono.error(new RuntimeException("Unexpected error occurred", e));
                });
    }

    @Override
    public Flux<CustomerResponseDto> getAllCustomers(String requestID) {
        return customerRepository.findAll().map(this::mapToCustomerResponseDto);
    }

    @Override
    public Mono<CustomerResponseDto> updateCustomer(@Valid CustomerRequestDto customerDto, String customerId, String requestID) {
        log.info("Updating customer with ID: {} Request ID {}", customerId, requestID);
        if (customerDto == null || customerDto.getFirstName() == null || customerDto.getFirstName().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Invalid customer data"));
        }
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
                .flatMap(existingCustomer -> {
                    existingCustomer.setFirstName(customerDto.getFirstName());
                    existingCustomer.setLastName(customerDto.getLastName());
                    existingCustomer.setPhone(customerDto.getPhone());
                    existingCustomer.setEmail(customerDto.getEmail());
                    existingCustomer.setAddress(customerDto.getAddress());
                    existingCustomer.setDateOfBirth(customerDto.getDateOfBirth());
                    existingCustomer.setIncome(customerDto.getIncome());
                    return customerRepository.save(existingCustomer);
                })
                .doOnNext(updatedCustomer ->
                        log.info("Successfully updated customer with ID: {} with request ID {}", updatedCustomer.getCustomerId(), requestID)
                )
                .map(this::mapToCustomerResponseDto);
    }

    @Override
    public Mono<Void> deleteCustomer(String customerId, String requestID) {
        log.info("Deleting customer with ID: {} Request ID {}", customerId, requestID);
        if (customerId == null || customerId.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Invalid customer data"));
        }
        log.info("Deleting customer with ID: {}", customerId);
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
                .flatMap(customerRepository::delete)
                .doOnSuccess(unused -> log.info("Successfully deleted customer with ID: {} request ID {}", customerId, requestID));
    }

    private CustomerResponseDto mapToCustomerResponseDto(Customer customer) {
        return CustomerResponseDto.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phone(customer.getPhone())
                .build();
    }
}
