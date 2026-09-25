package com.hospitality.customer.service;

import com.hospitality.customer.dto.AddressDto;
import com.hospitality.customer.dto.CustomerCreateRequest;
import com.hospitality.customer.dto.CustomerResponse;
import com.hospitality.customer.dto.CustomerUpdateRequest;
import com.hospitality.customer.entity.Address;
import com.hospitality.customer.entity.CustomerProfile;
import com.hospitality.customer.exception.BadRequestException;
import com.hospitality.customer.exception.ResourceNotFoundException;
import com.hospitality.customer.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerProfileRepository customerRepository;

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        log.info("Creating customer profile for userId: {}, email: {}", request.getUserId(), request.getEmail());

        if (customerRepository.existsByUserId(request.getUserId())) {
            throw new BadRequestException("Customer profile already exists for userId: " + request.getUserId());
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Customer with email already exists: " + request.getEmail());
        }

        Address address = null;
        if (request.getAddress() != null) {
            address = Address.builder()
                    .street(request.getAddress().getStreet())
                    .city(request.getAddress().getCity())
                    .state(request.getAddress().getState())
                    .postalCode(request.getAddress().getPostalCode())
                    .country(request.getAddress().getCountry())
                    .build();
        }

        CustomerProfile profile = CustomerProfile.builder()
                .userId(request.getUserId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .idProofType(request.getIdProofType())
                .idProofNumber(request.getIdProofNumber())
                .address(address)
                .preferences(request.getPreferences())
                .isActive(true)
                .build();

        CustomerProfile saved = customerRepository.save(profile);
        log.info("Customer profile created with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        CustomerProfile profile = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByUserId(Long userId) {
        CustomerProfile profile = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with userId: " + userId));
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        log.info("Updating customer profile ID: {}", id);
        CustomerProfile profile = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + id));

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) profile.setPhoneNumber(request.getPhoneNumber());
        if (request.getIdProofType() != null) profile.setIdProofType(request.getIdProofType());
        if (request.getIdProofNumber() != null) profile.setIdProofNumber(request.getIdProofNumber());
        if (request.getPreferences() != null) profile.setPreferences(request.getPreferences());
        if (request.getIsActive() != null) profile.setIsActive(request.getIsActive());

        if (request.getAddress() != null) {
            Address address = Address.builder()
                    .street(request.getAddress().getStreet())
                    .city(request.getAddress().getCity())
                    .state(request.getAddress().getState())
                    .postalCode(request.getAddress().getPostalCode())
                    .country(request.getAddress().getCountry())
                    .build();
            profile.setAddress(address);
        }

        CustomerProfile updated = customerRepository.save(profile);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateCustomerActive(Long id) {
        return customerRepository.findById(id)
                .map(CustomerProfile::getIsActive)
                .orElse(false);
    }

    private CustomerResponse mapToResponse(CustomerProfile profile) {
        AddressDto addressDto = null;
        if (profile.getAddress() != null) {
            addressDto = AddressDto.builder()
                    .street(profile.getAddress().getStreet())
                    .city(profile.getAddress().getCity())
                    .state(profile.getAddress().getState())
                    .postalCode(profile.getAddress().getPostalCode())
                    .country(profile.getAddress().getCountry())
                    .build();
        }

        return CustomerResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getEmail())
                .phoneNumber(profile.getPhoneNumber())
                .idProofType(profile.getIdProofType())
                .idProofNumber(profile.getIdProofNumber())
                .address(addressDto)
                .preferences(profile.getPreferences())
                .isActive(profile.getIsActive())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
