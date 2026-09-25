package com.hospitality.booking.client;

import com.hospitality.booking.dto.ApiResponse;
import com.hospitality.booking.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceClientDelegateTest {

    @Mock
    private CustomerClient customerClient;

    @InjectMocks
    private CustomerServiceClientDelegate delegate;

    @Test
    @DisplayName("Should successfully validate customer when customer-service is available")
    void testValidateCustomerActive_Success() {
        when(customerClient.validateCustomerActive(1L)).thenReturn(ApiResponse.success("Success", true));

        boolean isValid = delegate.validateCustomerActive(1L);

        assertTrue(isValid);
        verify(customerClient).validateCustomerActive(1L);
    }

    @Test
    @DisplayName("Should return false when customer-service reports inactive customer")
    void testValidateCustomerActive_Inactive() {
        when(customerClient.validateCustomerActive(2L)).thenReturn(ApiResponse.success("Inactive", false));

        boolean isValid = delegate.validateCustomerActive(2L);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should execute circuit breaker OPEN fallback and throw ServiceUnavailableException (HTTP 503)")
    void testValidateCustomerFallback_CallNotPermitted() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("customerServiceCircuitBreaker");
        CallNotPermittedException openEx = CallNotPermittedException.createCallNotPermittedException(cb);

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.validateCustomerFallback(1L, openEx)
        );

        assertTrue(ex.getMessage().contains("circuit breaker is OPEN"));
    }

    @Test
    @DisplayName("Should execute generic fallback when customer-service connection fails")
    void testValidateCustomerFallback_ConnectionError() {
        RuntimeException downstreamError = new RuntimeException("Connection reset by peer :8082");

        ServiceUnavailableException ex = assertThrows(
                ServiceUnavailableException.class,
                () -> delegate.validateCustomerFallback(1L, downstreamError)
        );

        assertTrue(ex.getMessage().contains("unreachable"));
    }
}
