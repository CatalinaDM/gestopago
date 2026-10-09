package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Permitir registro público de clientes (Onboarding)
        String uri = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod()) && (uri.endsWith("/clientes") || uri.endsWith("/clientes/"))) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            escribirError(response, HttpStatus.UNAUTHORIZED, "AUTH-003", "Token de autorización requerido en formato Bearer");
            return false;
        }

        String token = authHeader.substring(7).trim();
        if (!jwtUtil.validarToken(token)) {
            escribirError(response, HttpStatus.UNAUTHORIZED, "AUTH-006", "Token de autorización inválido o expirado");
            return false;
        }

        request.setAttribute("usuarioCorreo", jwtUtil.obtenerCorreo(token));
        request.setAttribute("usuarioId", jwtUtil.obtenerUsuarioId(token));
        request.setAttribute("clienteId", jwtUtil.obtenerClienteId(token));
        request.setAttribute("rol", jwtUtil.obtenerRol(token));

        return true;
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String codigo, String mensaje) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse error = new ApiErrorResponse(codigo, mensaje);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
