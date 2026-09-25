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
    public ResponseEntity<ApiResponse<BillResponse>> getBillById(@PathVariable("id") Long id) {
        BillResponse response = billingService.getBillById(id);
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
    public ResponseEntity<ApiResponse<List<BillResponse>>> getBillsByCustomerId(@PathVariable("customerId") Long customerId) {
        List<BillResponse> bills = billingService.getBillsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success("Customer bills retrieved", bills));
    }

    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<ApiResponse<List<BillResponse>>> getBillsByHotelId(@PathVariable("hotelId") Long hotelId) {
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
            @RequestParam(value = "reason", defaultValue = "Booking cancellation") String reason) {
        BillResponse response = billingService.processRefund(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }
}
