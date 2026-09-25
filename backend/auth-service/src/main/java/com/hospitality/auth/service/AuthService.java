package com.hospitality.auth.service;

import com.hospitality.auth.dto.AuthResponse;
import com.hospitality.auth.dto.LoginRequest;
import com.hospitality.auth.dto.RegisterRequest;
import com.hospitality.auth.dto.UserDto;

public interface AuthService {

    AuthResponse login(LoginRequest loginRequest);

    UserDto register(RegisterRequest registerRequest);

    UserDto getCurrentUser(Long userId);

    boolean validateToken(String token);
}
