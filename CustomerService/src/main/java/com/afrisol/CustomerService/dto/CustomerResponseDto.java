package com.afrisol.CustomerService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerResponseDto {
        private String customerId;
        private String firstName;
        private String lastName;
        private String phone;
}
