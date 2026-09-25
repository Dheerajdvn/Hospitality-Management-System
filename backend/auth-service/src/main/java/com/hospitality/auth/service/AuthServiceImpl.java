package com.hospitality.auth.service;

import com.hospitality.auth.dto.AuthResponse;
import com.hospitality.auth.dto.LoginRequest;
import com.hospitality.auth.dto.RegisterRequest;
import com.hospitality.auth.dto.UserDto;
import com.hospitality.auth.entity.Role;
import com.hospitality.auth.entity.RoleName;
import com.hospitality.auth.entity.User;
import com.hospitality.auth.exception.BadRequestException;
import com.hospitality.auth.exception.ResourceNotFoundException;
import com.hospitality.auth.repository.RoleRepository;
import com.hospitality.auth.repository.UserRepository;
import com.hospitality.auth.security.JwtTokenProvider;
import com.hospitality.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public UserDto register(RegisterRequest request) {
        log.info("Processing user registration for email: {}, username: {}", request.getEmail(), request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use: " + request.getEmail());
        }

        RoleName targetRole = resolveRole(request.getRole());
        Role role = roleRepository.findByName(targetRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + targetRole));

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isActive(true)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return mapToUserDto(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Authenticating user with identifier: {}", request.getUsernameOrEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("User ID: {} logged in successfully with roles: {}", userPrincipal.getId(), roles);

        return AuthResponse.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .userId(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return mapToUserDto(user);
    }

    @Override
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    private UserDto mapToUserDto(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private RoleName resolveRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return RoleName.ROLE_CUSTOMER;
        }
        try {
            String formatted = roleStr.toUpperCase();
            if (!formatted.startsWith("ROLE_")) {
                formatted = "ROLE_" + formatted;
            }
            return RoleName.valueOf(formatted);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role requested: {}. Falling back to ROLE_CUSTOMER", roleStr);
            return RoleName.ROLE_CUSTOMER;
        }
    }
}
