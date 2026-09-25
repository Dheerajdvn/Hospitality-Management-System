package com.hospitality.billing.service;

import com.hospitality.billing.dto.BillResponse;
import com.hospitality.billing.dto.InvoiceResponse;
import com.hospitality.billing.dto.PaymentRequest;

import java.util.List;

public interface BillingService {

    BillResponse processPayment(PaymentRequest request);

    BillResponse getBillById(Long id);

    BillResponse getBillByInvoiceNumber(String invoiceNumber);

    BillResponse getBillByBookingId(Long bookingId);

    List<BillResponse> getBillsByCustomerId(Long customerId);

    List<BillResponse> getBillsByHotelId(Long hotelId);

    InvoiceResponse getInvoice(Long billId);

    BillResponse processRefund(Long billId, String reason);
}
