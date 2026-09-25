package com.hospitality.customer.repository;

import com.hospitality.customer.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUserId(Long userId);

    Optional<CustomerProfile> findByEmail(String email);

    boolean existsByUserId(Long userId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
