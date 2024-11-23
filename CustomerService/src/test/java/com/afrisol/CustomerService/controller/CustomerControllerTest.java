package com.afrisol.CustomerService.controller;

import com.afrisol.CustomerService.dto.CustomerRequestDto;
import com.afrisol.CustomerService.exception.CustomerNotFoundException;
import com.afrisol.CustomerService.dto.CustomerResponseDto;
import com.afrisol.CustomerService.exception.CustomerAlreadyExistsException;
import com.afrisol.CustomerService.model.Address;
import com.afrisol.CustomerService.model.Customer;
import com.afrisol.CustomerService.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(CustomerController.class)
public class CustomerControllerTest {
    @MockBean
    private CustomerService customerService;

    @Autowired
    private WebTestClient webTestClient;
    private Customer customer;
    private Customer customer2;
    private CustomerRequestDto customerDto;
    private CustomerResponseDto customerResponseDto;
    private CustomerResponseDto customerResponseDto2;
    private String requestID;

    @BeforeEach
    void setUp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        customer = new Customer();
        customer.setCustomerId("testId");
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("test@test.com");
        customer.setPhone("testPhone");

        customer2 = new Customer();
        customer2.setCustomerId("testId2");
        customer2.setFirstName("Riche");
        customer2.setLastName("Smith");
        customer2.setEmail("riche@test.com");
        customer2.setPhone("testPhone2");

        customerDto = new CustomerRequestDto();
        customerDto.setFirstName("John");
        customerDto.setLastName("Doe");
        customerDto.setEmail("test@test.com");
        customerDto.setPhone("testPhone");
        customerDto.setDateOfBirth(LocalDate.parse("11/11/1980", formatter)); // Use formatter
        customerDto.setIncome(10000.0);

        Address address = new Address();
        address.setStreet("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        customerDto.setAddress(address);

        customerResponseDto = new CustomerResponseDto();
        customerResponseDto.setFirstName("John");
        customerResponseDto.setLastName("Doe");
        customerResponseDto.setCustomerId("testId");
        customerResponseDto.setPhone("testPhone");


        customerResponseDto2 = new CustomerResponseDto();
        customerResponseDto2.setFirstName("Riche");
        customerResponseDto2.setLastName("Smith");
        customerResponseDto2.setCustomerId("testId2");
        customerResponseDto2.setPhone("testPhone2");


        requestID = UUID.randomUUID().toString();

    }

    @Test
    void addCustomer_whenValid_shouldReturn200() {
        when(customerService.addCustomer(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(customerResponseDto));

        webTestClient.post()
                .uri("/api/v1/customers")
                .bodyValue(customerDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerResponseDto.class)
                .isEqualTo(customerResponseDto);

        Mockito.verify(customerService).addCustomer(Mockito.any(), Mockito.any());
    }

    @Test
    void addCustomer_whenInvalid_shouldReturn404() {
        when(customerService.addCustomer(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new CustomerAlreadyExistsException("Customer already exists!")));

        webTestClient.post()
                .uri("/api/v1/customers")
                .bodyValue(customerDto)
                .exchange()
                .expectStatus().is4xxClientError();
        Mockito.verify(customerService).addCustomer(Mockito.any(), Mockito.any());
    }

    @Test
    void getCustomerByEmail_whenValid_shouldReturn200() {
        when(customerService.getCustomer(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(customerResponseDto));
        webTestClient.get()
                .uri("/api/v1/customers/testId")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerResponseDto.class);
        Mockito.verify(customerService).getCustomer(Mockito.any(), Mockito.any());
    }

    @Test
    void getCustomerByEmail_whenInValid_shouldReturn400() {
        when(customerService.getCustomer(Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new RuntimeException("Customer not Found")));
        webTestClient.get()
                .uri("/api/v1/customers/testIdNotValid")
                .exchange()
                .expectStatus().is4xxClientError();
        Mockito.verify(customerService).getCustomer(Mockito.any(), Mockito.any());
    }

    @Test
    void getAllCustomers_whenValid_shouldReturn200() {
        List<CustomerResponseDto> customers = Arrays.asList(customerResponseDto, customerResponseDto2);
        when(customerService.getAllCustomers(Mockito.any()))
                .thenReturn(Flux.fromIterable(customers));
        webTestClient.get()
                .uri("/api/v1/customers")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CustomerResponseDto.class);

        Mockito.verify(customerService).getAllCustomers(Mockito.any());

    }



    @Test
    void updateCustomer_whenCustomerExists_shouldReturnUpdatedCustomer() {
        when(customerService.updateCustomer(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(customerResponseDto));

        webTestClient.put()
                .uri("/api/v1/customers/{customerId}", "testId")
                .bodyValue(customerDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerResponseDto.class)
                .isEqualTo(customerResponseDto);

        Mockito.verify(customerService).updateCustomer(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void updateCustomer_whenCustomerNotFound_shouldReturn404() {
        when(customerService.updateCustomer(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new CustomerNotFoundException("Customer not found with ID: testId")));

        webTestClient.put()
                .uri("/api/v1/customers/{customerId}", "testId")
                .bodyValue(customerDto)
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(customerService).updateCustomer(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void updateCustomer_whenInvalidData_shouldReturn400() {
        customerDto.setFirstName(""); // Invalid first name
        when(customerService.updateCustomer(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid customer data")));

        webTestClient.put()
                .uri("/api/v1/customers/{customerId}", "testId")
                .bodyValue(customerDto)
                .exchange()
                .expectStatus().isBadRequest();

        Mockito.verify(customerService).updateCustomer(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void updateCustomer_whenDatabaseErrorOccurs_shouldReturn500() {
        when(customerService.updateCustomer(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        webTestClient.put()
                .uri("/api/v1/customers/{customerId}", "testId")
                .bodyValue(customerDto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Mockito.verify(customerService).updateCustomer(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void deleteCustomer_whenCustomerExists_shouldReturnNoContent() {
        when(customerService.deleteCustomer(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/customers/{customerId}", "testId")
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(customerService).deleteCustomer(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void deleteCustomer_whenCustomerNotFound_shouldReturn404() {
        when(customerService.deleteCustomer(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.error(new CustomerNotFoundException("Customer not found with ID: testId")));

        webTestClient.delete()
                .uri("/api/v1/customers/{customerId}", "testId")
                .exchange()
                .expectStatus().isNotFound();

        Mockito.verify(customerService).deleteCustomer(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void deleteCustomer_whenDatabaseErrorOccurs_shouldReturn500() {
        when(customerService.deleteCustomer(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        webTestClient.delete()
                .uri("/api/v1/customers/{customerId}", "testId")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

        Mockito.verify(customerService).deleteCustomer(Mockito.anyString(), Mockito.anyString());
    }

}
