package com.hospitality.billing.repository;

import com.hospitality.billing.entity.Bill;
import com.hospitality.billing.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByInvoiceNumber(String invoiceNumber);

    Optional<Bill> findByTransactionReference(String transactionReference);

    Optional<Bill> findByBookingId(Long bookingId);

    Optional<Bill> findByBookingReference(String bookingReference);

    List<Bill> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Bill> findByHotelIdOrderByCreatedAtDesc(Long hotelId);

    boolean existsByBookingIdAndStatus(Long bookingId, BillStatus status);
}
