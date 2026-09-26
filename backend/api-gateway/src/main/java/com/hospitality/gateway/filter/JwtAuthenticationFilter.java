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
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        log.debug("API Gateway evaluating request: {} {}", method, path);

        // 1. Whitelist Check (Public endpoints bypass JWT verification)
        if (isWhitelisted(path, method)) {
            log.debug("Path '{}' is whitelisted. Bypassing JWT filter.", path);
            boolean hasSpoofedHeaders = request.getHeaders().containsKey("X-User-Id")
                    || request.getHeaders().containsKey("X-User-Role")
                    || request.getHeaders().containsKey("X-User-Roles")
                    || request.getHeaders().containsKey("X-User-Email")
                    || request.getHeaders().containsKey("X-User-Name");
            if (hasSpoofedHeaders) {
                ServerHttpRequest sanitized = request.mutate()
                        .headers(httpHeaders -> {
                            httpHeaders.remove("X-User-Id");
                            httpHeaders.remove("X-User-Email");
                            httpHeaders.remove("X-User-Name");
                            httpHeaders.remove("X-User-Roles");
                            httpHeaders.remove("X-User-Role");
                        })
                        .build();
                return chain.filter(exchange.mutate().request(sanitized).build());
            }
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

        // 4. Extract Claims
        Long userId = jwtUtils.getUserId(token);
        String email = jwtUtils.getEmail(token);
        String username = jwtUtils.getUsername(token);
        List<String> roles = jwtUtils.getRoles(token);
        String rolesStr = roles != null ? String.join(",", roles) : "";
        String primaryRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : "";

        log.debug("Authenticated user ID: {}, email: {}, username: {}, roles: {}", userId, email, username, rolesStr);

        // 5. Route-level Role-Based Access Control (RBAC) Enforcement
        if (isAdminOnlyEndpoint(path, method)) {
            if (roles == null || !roles.contains("ROLE_ADMIN")) {
                log.warn("Access denied (403): User {} with roles '{}' attempted access to admin endpoint: {} {}",
                        userId, rolesStr, method, path);
                return forbiddenResponse(exchange, "Access denied: Administrator privileges required.");
            }
        } else if (isStaffOrAdminEndpoint(path, method)) {
            if (roles == null || (!roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_STAFF"))) {
                log.warn("Access denied (403): User {} with roles '{}' attempted access to staff/admin endpoint: {} {}",
                        userId, rolesStr, method, path);
                return forbiddenResponse(exchange, "Access denied: Staff or Administrator privileges required.");
            }
        }

        // 6. Mutate Request with Downstream Headers (overwriting any client-supplied spoofed headers)
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

        // Public auth endpoints (/api/v1/auth/me is protected)
        if (path.equals("/api/v1/auth/login") ||
            path.equals("/api/v1/auth/register") ||
            path.equals("/api/v1/auth/validate")) {
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

        // Public read-only browsing (hotels, rooms, food catalog)
        if (HttpMethod.GET.equals(method)) {
            if (path.startsWith("/api/v1/hotels") ||
                path.startsWith("/api/v1/rooms") ||
                path.startsWith("/api/v1/food")) {
                return true;
            }
        }

        return false;
    }

    private boolean isAdminOnlyEndpoint(String path, HttpMethod method) {
        // Hotel catalog modifications
        if (path.startsWith("/api/v1/hotels") && (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.DELETE.equals(method))) {
            return true;
        }
        // Room deletion
        if (path.startsWith("/api/v1/rooms") && HttpMethod.DELETE.equals(method)) {
            return true;
        }
        // Food item deletion
        if (path.startsWith("/api/v1/food") && HttpMethod.DELETE.equals(method)) {
            return true;
        }
        // Payment refunds
        if (path.startsWith("/api/v1/billing") && path.endsWith("/refund")) {
            return true;
        }
        // Simulate notification event
        if (path.startsWith("/api/v1/notifications/simulate-event")) {
            return true;
        }
        return false;
    }

    private boolean isStaffOrAdminEndpoint(String path, HttpMethod method) {
        // Room creation, updates, and operational status transitions
        if (path.startsWith("/api/v1/rooms") && (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method))) {
            return true;
        }
        // Food item creation, updates, and availability toggles (except batch lookup)
        if (path.startsWith("/api/v1/food") && !path.contains("/batch") &&
                (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method))) {
            return true;
        }
        // Warehouse inventory management & stock updates
        if (path.startsWith("/api/v1/inventory") && (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method) || HttpMethod.PATCH.equals(method))) {
            return true;
        }
        // Kitchen order ticket (KOT) status advancement by staff
        if (path.startsWith("/api/v1/room-service/orders") && (HttpMethod.PATCH.equals(method) || (HttpMethod.PUT.equals(method) && path.endsWith("/status")))) {
            return true;
        }
        // Direct notification dispatching
        if (path.startsWith("/api/v1/notifications/send-direct")) {
            return true;
        }
        return false;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        return buildErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", message);
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        return buildErrorResponse(exchange, HttpStatus.FORBIDDEN, "Forbidden", message);
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, HttpStatus status, String error, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null && isAllowedOrigin(origin)) {
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        String json = String.format(
                "{\"success\":false,\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                status.value(), error, message, LocalDateTime.now()
        );

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isAllowedOrigin(String origin) {
        return ALLOWED_ORIGINS.contains(origin) || origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:");
    }

    @Override
    public int getOrder() {
        return -100; // Run early in filter chain before route forwarding
    }
}
