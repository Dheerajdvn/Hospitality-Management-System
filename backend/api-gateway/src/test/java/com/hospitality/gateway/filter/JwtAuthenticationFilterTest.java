package com.hospitality.gateway.filter;

import com.hospitality.gateway.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private GatewayFilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtils);
    }

    @Test
    @DisplayName("Should bypass JWT filter for whitelisted auth endpoints")
    void filter_WhitelistedAuthPath_BypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, filterChain);
        result.block();

        verify(filterChain, times(1)).filter(exchange);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("Should bypass JWT filter for public GET hotels endpoint")
    void filter_WhitelistedHotelGetPath_BypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/hotels/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, filterChain);
        result.block();

        verify(filterChain, times(1)).filter(exchange);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("Should bypass JWT filter for CORS OPTIONS preflight request")
    void filter_CorsPreflight_BypassesFilter() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/bookings").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, filterChain);
        result.block();

        verify(filterChain, times(1)).filter(exchange);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when Authorization header is missing on protected endpoint")
    void filter_ProtectedEndpoint_MissingAuthHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/bookings").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, filterChain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when JWT token is invalid")
    void filter_ProtectedEndpoint_InvalidToken() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(jwtUtils.validateToken("invalid.jwt.token")).thenReturn(false);

        Mono<Void> result = filter.filter(exchange, filterChain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verifyNoInteractions(filterChain);
    }

    @Test
    @DisplayName("Should validate token and inject downstream headers (X-User-Id, X-User-Email, X-User-Roles)")
    void filter_ProtectedEndpoint_ValidToken_InjectsHeaders() {
        String token = "valid.jwt.token";
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getUserId(token)).thenReturn(42L);
        when(jwtUtils.getEmail(token)).thenReturn("guest@hospitality.com");
        when(jwtUtils.getUsername(token)).thenReturn("guestuser");
        when(jwtUtils.getRoles(token)).thenReturn(List.of("ROLE_CUSTOMER"));

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        Mono<Void> result = filter.filter(exchange, filterChain);
        result.block();

        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        HttpHeaders capturedHeaders = capturedExchange.getRequest().getHeaders();

        assertEquals("42", capturedHeaders.getFirst("X-User-Id"));
        assertEquals("guest@hospitality.com", capturedHeaders.getFirst("X-User-Email"));
        assertEquals("guestuser", capturedHeaders.getFirst("X-User-Name"));
        assertEquals("ROLE_CUSTOMER", capturedHeaders.getFirst("X-User-Roles"));
        assertEquals("ROLE_CUSTOMER", capturedHeaders.getFirst("X-User-Role"));
    }

    @Test
    @DisplayName("Should reflect Origin header on 401 Unauthorized response to prevent browser CORS blocking")
    void filter_Unauthorized_PreservesCorsOriginHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/bookings")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, filterChain);
        result.block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals("http://localhost:5173", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("true", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }
}

