package com.hospitality.customer.service;

import com.hospitality.customer.dto.AddressDto;
import com.hospitality.customer.dto.CustomerCreateRequest;
import com.hospitality.customer.dto.CustomerResponse;
import com.hospitality.customer.entity.Address;
import com.hospitality.customer.entity.CustomerProfile;
import com.hospitality.customer.exception.BadRequestException;
import com.hospitality.customer.exception.ResourceNotFoundException;
import com.hospitality.customer.repository.CustomerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerProfileRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CustomerProfile sampleProfile;

    @BeforeEach
    void setUp() {
        Address address = Address.builder()
                .street("100 Luxury Blvd")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .build();

        sampleProfile = CustomerProfile.builder()
                .id(1L)
                .userId(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0100")
                .idProofType("PASSPORT")
                .idProofNumber("P123456789")
                .address(address)
                .preferences("High floor")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a customer profile")
    void testCreateCustomerSuccess() {
        AddressDto addressDto = AddressDto.builder()
                .street("100 Luxury Blvd")
                .city("New York")
                .state("NY")
                .postalCode("10001")
                .country("USA")
                .build();

        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .userId(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0100")
                .address(addressDto)
                .build();

        when(customerRepository.existsByUserId(10L)).thenReturn(false);
        when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(customerRepository.save(any(CustomerProfile.class))).thenReturn(sampleProfile);

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");

        verify(customerRepository, times(1)).save(any(CustomerProfile.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if customer profile already exists for userId")
    void testCreateCustomerDuplicateUserId() {
        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .userId(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0100")
                .build();

        when(customerRepository.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Customer profile already exists for userId");

        verify(customerRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException if customer with email already exists")
    void testCreateCustomerDuplicateEmail() {
        CustomerCreateRequest request = CustomerCreateRequest.builder()
                .userId(11L)
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0100")
                .build();

        when(customerRepository.existsByUserId(11L)).thenReturn(false);
        when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Customer with email already exists");

        verify(customerRepository, never()).save(any(CustomerProfile.class));
    }

    @Test
    @DisplayName("Should return customer profile by ID")
    void testGetCustomerByIdSuccess() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleProfile));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer ID is not found")
    void testGetCustomerByIdNotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with ID");
    }

    @Test
    @DisplayName("Should return true when customer is active")
    void testValidateCustomerActive() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleProfile));

        boolean isActive = customerService.validateCustomerActive(1L);

        assertThat(isActive).isTrue();
    }
}
