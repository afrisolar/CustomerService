package com.afrisol.CustomerService.service;

import com.afrisol.CustomerService.dto.CustomerRequest;
import com.afrisol.CustomerService.dto.CustomerResponse;
import com.afrisol.CustomerService.exception.CustomException;
import com.afrisol.CustomerService.exception.CustomerAlreadyExistsException;
import com.afrisol.CustomerService.exception.CustomerNotFoundException;
import com.afrisol.CustomerService.model.Customer;
import com.afrisol.CustomerService.repository.CustomerRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Mono<CustomerResponse> addCustomer(CustomerRequest customerRequest, String requestID) {
        if (customerRequest == null) {
            return Mono.error(new IllegalArgumentException("CustomerRequest cannot be null"));
        }

        return customerRepository.findByPhone(customerRequest.getPhone())
                .flatMap(existingCustomer ->
                        Mono.error(new CustomerAlreadyExistsException("Customer already exists"))
                )
                .switchIfEmpty(
                        customerRepository.save(Customer.builder()
                                        .address(customerRequest.getAddress())
                                        .email(customerRequest.getEmail())
                                        .phone(customerRequest.getPhone())
                                        .lastName(customerRequest.getLastName())
                                        .firstName(customerRequest.getFirstName())
                                        .dateOfBirth(customerRequest.getDateOfBirth())
                                        .income(customerRequest.getIncome())
                                        .build()
                                )
                                .doOnNext(savedCustomer ->
                                        log.info("Successfully added customer with ID: {} and requestID: {}",
                                                savedCustomer.getCustomerId(), requestID)
                                )
                                .map(this::mapToCustomerResponseDto) // Map saved Customer to CustomerResponse
                )
                .cast(CustomerResponse.class);
    }

    @Override
    public Mono<CustomerResponse> getCustomer(String phone, String requestID) {
        if (phone == null || phone.isEmpty()) {
            return Mono.error(new CustomException(HttpStatus.BAD_REQUEST,"Phone cannot be null"));
        }
        log.info("Searching for customer with phoneNumber: {}", phone);
        return customerRepository.findByPhone(phone)
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
    public Flux<CustomerResponse> getAllCustomers(String requestID) {
        return customerRepository.findAll().map(this::mapToCustomerResponseDto);
    }

    @Override
    public Mono<CustomerResponse> updateCustomer(@Valid CustomerRequest customerDto, String customerId, String requestID) {
        log.info("Updating customer with ID: {} Request ID {}", customerId, requestID);
        if (customerDto == null || customerDto.getPhone() == null || customerDto.getFirstName().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Invalid customer data"));
        }
        return customerRepository.findByCustomerId(customerId)
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
        return customerRepository.findByCustomerId(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException("Customer not found with ID: " + customerId)))
                .flatMap(customerRepository::delete)
                .doOnSuccess(unused -> log.info("Successfully deleted customer with ID: {} request ID {}", customerId, requestID));
    }

    private CustomerResponse mapToCustomerResponseDto(Customer customer) {
        return CustomerResponse.builder()
                .customerId(customer.getCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phone(customer.getPhone())
                .build();
    }
}
