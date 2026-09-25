package com.hospitality.auth.service;

import com.hospitality.auth.dto.AuthResponse;
import com.hospitality.auth.dto.LoginRequest;
import com.hospitality.auth.dto.RegisterRequest;
import com.hospitality.auth.dto.UserDto;
import com.hospitality.auth.entity.Role;
import com.hospitality.auth.entity.RoleName;
import com.hospitality.auth.entity.User;
import com.hospitality.auth.exception.BadRequestException;
import com.hospitality.auth.repository.RoleRepository;
import com.hospitality.auth.repository.UserRepository;
import com.hospitality.auth.security.JwtTokenProvider;
import com.hospitality.auth.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role customerRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        customerRole = Role.builder()
                .id(1L)
                .name(RoleName.ROLE_CUSTOMER)
                .description("Customer Role")
                .build();

        testUser = User.builder()
                .id(100L)
                .username("johndoe")
                .email("john@example.com")
                .password("encoded_secret_pwd")
                .firstName("John")
                .lastName("Doe")
                .isActive(true)
                .roles(Set.of(customerRole))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new customer")
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("rawPassword123")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encoded_secret_pwd");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getRoles()).contains("ROLE_CUSTOMER");

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when registering duplicate username")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("rawPassword123")
                .build();

        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when registering duplicate email")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("john@example.com")
                .password("rawPassword123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate user and return JWT token")
    void testLoginSuccess() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("johndoe")
                .password("rawPassword123")
                .build();

        UserPrincipal principal = UserPrincipal.builder()
                .id(100L)
                .username("johndoe")
                .email("john@example.com")
                .password("encoded_secret_pwd")
                .isActive(true)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getUsername()).isEqualTo("johndoe");
        assertThat(response.getRoles()).contains("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should return true when token is valid")
    void testValidateTokenValid() {
        when(tokenProvider.validateToken("valid.token")).thenReturn(true);

        boolean isValid = authService.validateToken("valid.token");

        assertThat(isValid).isTrue();
        verify(tokenProvider, times(1)).validateToken("valid.token");
    }
}
