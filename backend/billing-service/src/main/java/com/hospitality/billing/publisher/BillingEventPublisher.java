package com.hospitality.billing.publisher;

import com.hospitality.billing.event.PaymentCompletedEvent;
import com.hospitality.billing.event.PaymentFailedEvent;
import com.hospitality.billing.event.PaymentProcessedEvent;
import com.hospitality.billing.event.RefundProcessedEvent;

public interface BillingEventPublisher {

    void publishPaymentProcessed(PaymentProcessedEvent event);

    void publishPaymentCompleted(PaymentCompletedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);

    void publishRefundProcessed(RefundProcessedEvent event);
}

