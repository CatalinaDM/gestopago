package com.proyecto.servicios.model.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomicilioDTO {

    @NotBlank(message = "La calle es obligatoria")
    @Size(min = 2, max = 100, message = "La calle debe tener entre 2 y 100 caracteres")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 20, message = "El número exterior no debe exceder 20 caracteres")
    private String numeroExterior;

    @Size(max = 20, message = "El número interior no debe exceder 20 caracteres")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(min = 2, max = 100, message = "La colonia debe tener entre 2 y 100 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio o alcaldía es obligatorio")
    @Size(min = 2, max = 100, message = "El municipio debe tener entre 2 y 100 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(min = 2, max = 100, message = "El estado debe tener entre 2 y 100 caracteres")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Size(min = 2, max = 50, message = "El país debe tener entre 2 y 50 caracteres")
    private String pais;
}
