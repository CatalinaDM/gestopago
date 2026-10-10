package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApi {
    @Bean
    public OpenAPI openAPI(){
        return new OpenAPI().servers(List.of(
                new Server().url("/").description("Servidor actual (Render / Producción / Local)"),
                new Server().url("http://localhost:8081").description("Localhost 8081")
        ));
    }
}
