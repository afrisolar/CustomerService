package com.afrisol.CustomerService.controller;

import com.afrisol.CustomerService.dto.CustomerRequest;
import com.afrisol.CustomerService.dto.CustomerResponse;
import com.afrisol.CustomerService.exception.CustomerAlreadyExistsException;
import com.afrisol.CustomerService.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/customers")
@Slf4j
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public Mono<ResponseEntity<CustomerResponse>> addCustomer(@RequestBody @Valid CustomerRequest customerRequest) {
        String requestID = UUID.randomUUID().toString();
        log.info("Adding customer with phone: {} and requestID: {}", customerRequest.getPhone(), requestID);

        return customerService.addCustomer(customerRequest, requestID)
                .map(customerResponse -> ResponseEntity
                        .created(URI.create("/customers/" + customerResponse.getCustomerId()))
                        .body(customerResponse))
                .onErrorResume(e -> {
                    if (e instanceof CustomerAlreadyExistsException) {
                        return Mono.error(e);
                    }
                    log.error("Unexpected error adding customer: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping
    public Flux<CustomerResponse> getAllCustomers() {
        String requestID = UUID.randomUUID().toString();
        log.info("Getting all customers : {}", requestID);
        return customerService.getAllCustomers(requestID);
    }
    @GetMapping("/{phoneNumber}")
    public Mono<ResponseEntity<CustomerResponse>> getCustomer(@PathVariable  @Valid String phoneNumber) {
        String requestID = UUID.randomUUID().toString();
        log.info("Getting customer with email  : {} and requestID: {}", phoneNumber, requestID);
        return customerService.getCustomer(phoneNumber, requestID)
                .map(ResponseEntity::ok)
                .onErrorResume(ResponseStatusException.class, ex -> {
                    log.error("Error: {}", ex.getReason());
                    return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(null));
                })
                .onErrorResume(RuntimeException.class, ex ->
                        {
                            log.error("Unhandled RuntimeException: {}", ex.getMessage());
                            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
                        });
    }

    @PutMapping("/{customerId}")
    public Mono<ResponseEntity<CustomerResponse>> updateCustomer(
            @PathVariable String customerId,
            @RequestBody @Valid CustomerRequest customerRequest) {
        String requestID = UUID.randomUUID().toString();
        log.info("Updating customer with ID: {} and requestID {}", customerId, requestID);
        return customerService.updateCustomer(customerRequest, customerId, requestID)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }


    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete a customer by ID", responses = {
            @ApiResponse(responseCode = "200", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public Mono<ResponseEntity<Object>> deleteCustomer(@PathVariable String customerId) {
        String requestID = UUID.randomUUID().toString();
        log.info("Deleting customer with ID: {} and requestID {}", customerId, requestID);
        return customerService.deleteCustomer(customerId, requestID)
                .then(Mono.just(ResponseEntity.ok().header(HttpHeaders.CONTENT_LENGTH, "0").build()))
                .onErrorResume(e -> {
                    log.error("Error deleting customer with ID: {} - {}", customerId, e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(404).<Object>build()); // Error: 404 Not Found
                });
    }

}
