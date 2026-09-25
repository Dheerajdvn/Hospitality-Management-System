package com.hospitality.hotel.repository;

import com.hospitality.hotel.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {

    Page<Hotel> findByIsActiveTrue(Pageable pageable);

    Page<Hotel> findByCityIgnoreCaseAndIsActiveTrue(String city, Pageable pageable);

    Optional<Hotel> findByIdAndIsActiveTrue(Long id);
}
