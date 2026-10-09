package com.proyecto.servicios.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@Named("StringSanitizer")
public class StringSanitizer {

    @Named("trim")
    public String trim(String value) {
        return value != null ? value.trim() : null;
    }

    @Named("trimUpper")
    public String trimUpper(String value) {
        return value != null ? value.trim().toUpperCase() : null;
    }

    @Named("trimLower")
    public String trimLower(String value) {
        return value != null ? value.trim().toLowerCase() : null;
    }
}
