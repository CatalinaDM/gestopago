package com.proyecto.servicios.service;

import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
