package com.afrisol.CustomerService.service;

import com.afrisol.CustomerService.dto.CustomerRequestDto;
import com.afrisol.CustomerService.dto.CustomerResponseDto;
import com.afrisol.CustomerService.exception.CustomException;
import com.afrisol.CustomerService.exception.CustomerNotFoundException;
import com.afrisol.CustomerService.model.Customer;
import com.afrisol.CustomerService.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;
    private Customer customer;
    private Customer customer2;
    private Customer customer3;
    private CustomerRequestDto customerDto;
    private CustomerResponseDto customerResponseDto;
    private CustomerResponseDto customerResponseDto2;
    private String requestID;


    @BeforeEach
    void setUp() {
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
    void addCustomer() {
        Mockito.when(customerRepository.existsByEmail(Mockito.anyString())).thenReturn(Mono.just(false));
        when(customerRepository.save(Mockito.any(Customer.class))).thenAnswer(invocation -> {
            Customer savedCustomer = invocation.getArgument(0);
            Field idField = Customer.class.getDeclaredField("customerId");
            idField.setAccessible(true);
            idField.set(savedCustomer, "testId");
            savedCustomer.setPhone("testPhone");
            return Mono.just(savedCustomer);
        });

        StepVerifier.create(customerService.addCustomer(customerDto, requestID))
                .expectNext(customerResponseDto)
                .verifyComplete();
        verify(customerRepository, Mockito.times(1)).existsByEmail(customerDto.getEmail());
        verify(customerRepository, Mockito.times(1)).save(Mockito.any(Customer.class));
    }
    @Test
    void addCustomer_whenCustomerDtoIsNull_shouldThrowIllegalArgumentException() {
        // Arrange
        String requestID = "testRequestID";

        // Act & Assert
        StepVerifier.create(customerService.addCustomer(null, requestID))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("CustomerRequestDto cannot be null"))
                .verify();
    }

    @Test
    void addCustomer_whenEmailExists_shouldThrowError() {
        when(customerRepository.existsByEmail(customer.getEmail())).thenReturn(Mono.just(true));

        StepVerifier.create(customerService.addCustomer(customerDto, requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Customer already exists"))
                .verify();
        // Verify repository calls
        Mockito.verify(customerRepository).existsByEmail(customer.getEmail());
        Mockito.verify(customerRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void getCustomer_whenEmailExists() {
        when(customerRepository.findByEmail(customerDto.getEmail())).thenReturn(Mono.just(customer));

        StepVerifier.create(customerService.getCustomer(customerDto.getEmail(), requestID))
                .expectNextMatches(response ->
                        response.getFirstName().equals("John") &&
                                response.getPhone().equals("testPhone"))
                .verifyComplete();
        Mockito.verify(customerRepository, Mockito.times(1)).findByEmail(customer.getEmail());
    }

    @Test
    void getCustomer_whenEmailDoesNotExistShouldThrowError() {
        when(customerRepository.findByEmail(customerDto.getEmail())).thenReturn(Mono.empty());
        StepVerifier.create(customerService.getCustomer(customerDto.getEmail(), requestID))
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException &&
                                ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                                throwable.getMessage().contains("Customer not found"))
                .verify();
        Mockito.verify(customerRepository, Mockito.times(1)).findByEmail(customerDto.getEmail());
    }

    @Test
    void getCustomer_whenEmailIsNull_shouldThrowCustomException() {
        // Arrange
        String requestID = "testRequestID";

        // Act & Assert
        StepVerifier.create(customerService.getCustomer(null, requestID))
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException &&
                                ((CustomException) throwable).getStatus() == HttpStatus.BAD_REQUEST&&
                                throwable.getMessage().equals("Email cannot be null")
                )
                .verify();
    }



    @Test
    void getCustomers(){
        List<Customer> customers = Arrays.asList(customer, customer2);
        when(customerRepository.findAll()).thenReturn(Flux.fromIterable(customers));


        StepVerifier.create(customerService.getAllCustomers(requestID))
                .expectNextMatches( response ->
                    response.getFirstName().equals("John") &&
                            response.getLastName().equals("Doe") &&
                            response.getCustomerId().equals("testId")
                )
                .expectNextMatches(response ->
                        response.getFirstName().equals("Riche") &&
                                response.getLastName().equals("Smith") &&
                                response.getCustomerId().equals("testId2"))

                .verifyComplete();
        Mockito.verify(customerRepository, Mockito.times(1)).findAll();

    }

    @Test
    void updateCustomer_whenCustomerExists_shouldReturnUpdatedCustomer() {
        when(customerRepository.findById(Mockito.anyString())).thenReturn(Mono.just(customer));
        when(customerRepository.save(Mockito.any(Customer.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        customerDto.setFirstName("UpdatedName");
        customerDto.setLastName("UpdatedLastName");

        StepVerifier.create(customerService.updateCustomer(customerDto, "testId", requestID))
                .expectNextMatches(updatedCustomer -> updatedCustomer.getFirstName().equals("UpdatedName"))
                .verifyComplete();

        verify(customerRepository, Mockito.times(1)).findById("testId");
        verify(customerRepository, Mockito.times(1)).save(Mockito.any(Customer.class));
    }

    @Test
    void updateCustomer_whenCustomerNotFound_shouldThrowCustomerNotFoundException() {
        when(customerRepository.findById(Mockito.anyString())).thenReturn(Mono.empty());

        StepVerifier.create(customerService.updateCustomer(customerDto, "nonExistentId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException &&
                        throwable.getMessage().equals("Customer not found with ID: nonExistentId"))
                .verify();

        verify(customerRepository, Mockito.times(1)).findById("nonExistentId");
    }

    @Test
    void updateCustomer_whenInvalidData_shouldThrowValidationException() {
        customerDto.setFirstName(""); // Invalid first name

        StepVerifier.create(customerService.updateCustomer(customerDto, "testId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Invalid customer data"))
                .verify();

        // No repository interaction verification as the validation fails before repository is called
    }

    @Test
    void updateCustomer_whenDatabaseErrorOccurs_shouldThrowRuntimeException() {
        when(customerRepository.findById(Mockito.anyString())).thenReturn(Mono.just(customer));
        when(customerRepository.save(Mockito.any(Customer.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(customerService.updateCustomer(customerDto, "testId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();

        verify(customerRepository, Mockito.times(1)).findById("testId");
    }

    @Test
    void deleteCustomer_whenCustomerExists_shouldReturnVoid() {
        when(customerRepository.findById(Mockito.anyString())).thenReturn(Mono.just(customer));
        when(customerRepository.delete(Mockito.any(Customer.class))).thenReturn(Mono.empty());

        StepVerifier.create(customerService.deleteCustomer("testId", requestID))
                .verifyComplete();

        verify(customerRepository, Mockito.times(1)).findById("testId");
        verify(customerRepository, Mockito.times(1)).delete(Mockito.any(Customer.class));
    }

    @Test
    void deleteCustomer_whenCustomerNotFound_shouldThrowCustomerNotFoundException() {
        when(customerRepository.findById(Mockito.anyString())).thenReturn(Mono.empty());

        StepVerifier.create(customerService.deleteCustomer("nonExistentId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof CustomerNotFoundException &&
                        throwable.getMessage().equals("Customer not found with ID: nonExistentId"))
                .verify();

        verify(customerRepository, Mockito.times(1)).findById("nonExistentId");
    }

    @Test
    void deleteCustomer_whenDatabaseErrorOccurs_shouldThrowRuntimeException() {
        when(customerRepository.findById(Mockito.anyString())).thenReturn(Mono.just(customer));
        when(customerRepository.delete(Mockito.any(Customer.class))).thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(customerService.deleteCustomer("testId", requestID))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();

        verify(customerRepository, Mockito.times(1)).findById("testId");
        verify(customerRepository, Mockito.times(1)).delete(Mockito.any(Customer.class));
    }
}

