package com.hospitality.billing.controller;

import com.hospitality.billing.dto.ApiResponse;
import com.hospitality.billing.dto.BillResponse;
import com.hospitality.billing.dto.InvoiceResponse;
import com.hospitality.billing.dto.PaymentRequest;
import com.hospitality.billing.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<BillResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        BillResponse response = billingService.processPayment(request);
        return new ResponseEntity<>(
                ApiResponse.success("Payment processed successfully", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillResponse>> getBillById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long callerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String callerRoles) {
        BillResponse response = billingService.getBillById(id);
        boolean isAdmin = callerRoles != null && callerRoles.contains("ROLE_ADMIN");
        boolean isStaff = callerRoles != null && callerRoles.contains("ROLE_STAFF");
        boolean isOwner = callerUserId != null && callerUserId.equals(response.getCustomerId());
        if (!isAdmin && !isStaff && !isOwner && callerUserId != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You are not authorized to view this bill"));
        }
        return ResponseEntity.ok(ApiResponse.success("Bill details retrieved", response));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<BillResponse>> getBillByBookingId(@PathVariable("bookingId") Long bookingId) {
        BillResponse response = billingService.getBillByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved for booking", response));
    }

    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<ApiResponse<BillResponse>> getBillByInvoiceNumber(@PathVariable("invoiceNumber") String invoiceNumber) {
        BillResponse response = billingService.getBillByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved by invoice number", response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getBillsByCustomerId(
            @PathVariable("customerId") Long customerId,
            @RequestHeader(value = "X-User-Id", required = false) Long callerUserId,
            @RequestHeader(value = "X-User-Roles", required = false) String callerRoles) {
        boolean isAdmin = callerRoles != null && callerRoles.contains("ROLE_ADMIN");
        boolean isStaff = callerRoles != null && callerRoles.contains("ROLE_STAFF");
        boolean isOwner = callerUserId != null && callerUserId.equals(customerId);
        if (!isAdmin && !isStaff && !isOwner && callerUserId != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: You are not authorized to view bills for customer ID " + customerId));
        }
        List<BillResponse> bills = billingService.getBillsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer bills retrieved", bills));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getBillsByHotelId(
            @PathVariable("hotelId") Long hotelId,
            @RequestHeader(value = "X-User-Roles", required = false) String callerRoles) {
        if (callerRoles == null || (!callerRoles.contains("ROLE_ADMIN") && !callerRoles.contains("ROLE_STAFF"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: Staff or Administrator privileges required to view hotel revenue"));
        }
        List<BillResponse> bills = billingService.getBillsByHotelId(hotelId);
        return ResponseEntity.ok(ApiResponse.success("Hotel bills retrieved", bills));
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceReceipt(@PathVariable("id") Long id) {
        InvoiceResponse invoice = billingService.getInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Tax invoice receipt retrieved", invoice));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<BillResponse>> processRefund(
            @PathVariable("id") Long id,
            @RequestParam(value = "reason", defaultValue = "Booking cancellation") String reason,
            @RequestHeader(value = "X-User-Roles", required = false) String callerRoles) {
        if (callerRoles == null || !callerRoles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: Administrator privileges required to process refunds"));
        }
        BillResponse response = billingService.processRefund(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }
}
