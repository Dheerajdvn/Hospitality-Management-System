package com.hospitality.gateway.filter;

import com.hospitality.gateway.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        log.debug("API Gateway evaluating request: {} {}", method, path);

        // 1. Whitelist Check
        if (isWhitelisted(path, method)) {
            log.debug("Path '{}' is whitelisted. Bypassing JWT filter.", path);
            return chain.filter(exchange);
        }

        // 2. Extract Authorization Header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for protected endpoint: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // 3. Validate Token
        if (!jwtUtils.validateToken(token)) {
            log.warn("Invalid or expired JWT token for endpoint: {}", path);
            return unauthorizedResponse(exchange, "Invalid or expired JWT token");
        }

        // 4. Extract Claims and Mutate Request with Downstream Headers
        Long userId = jwtUtils.getUserId(token);
        String email = jwtUtils.getEmail(token);
        String username = jwtUtils.getUsername(token);
        List<String> roles = jwtUtils.getRoles(token);
        String rolesStr = roles != null ? String.join(",", roles) : "";
        String primaryRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : "";

        log.debug("Authenticated user ID: {}, email: {}, username: {}, roles: {}", userId, email, username, rolesStr);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId != null ? String.valueOf(userId) : "")
                .header("X-User-Email", email != null ? email : "")
                .header("X-User-Name", username != null ? username : "")
                .header("X-User-Roles", rolesStr)
                .header("X-User-Role", primaryRole)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhitelisted(String path, HttpMethod method) {
        // Permit CORS preflight
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }

        // Public auth endpoints
        if (path.startsWith("/api/v1/auth")) {
            return true;
        }

        // Actuator health & info
        if (path.startsWith("/actuator")) {
            return true;
        }

        // Swagger / OpenAPI documentation
        if (path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-ui") ||
            path.equals("/swagger-ui.html")) {
            return true;
        }

        // Static favicon
        if (path.equals("/favicon.ico")) {
            return true;
        }

        // Public read-only browsing
        if (HttpMethod.GET.equals(method)) {
            if (path.startsWith("/api/v1/hotels") ||
                path.startsWith("/api/v1/rooms") ||
                path.startsWith("/api/v1/food")) {
                return true;
            }
        }

        return false;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Preserve CORS headers on error responses so browser clients (e.g. Vite React on 5173) can read the 401
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null) {
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        String json = String.format(
                "{\"success\":false,\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                message, LocalDateTime.now()
        );

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100; // Run early in filter chain before route forwarding
    }
}
