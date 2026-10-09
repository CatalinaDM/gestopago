package com.proyecto.servicios.model.cliente;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRegistroRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El nombre solo debe contener letras, acentos, diéresis, guiones o apóstrofes")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]*$", message = "El segundo nombre solo debe contener letras, acentos, diéresis, guiones o apóstrofes")
    @Size(max = 50, message = "El segundo nombre no debe exceder 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido paterno solo debe contener letras, acentos, diéresis, guiones o apóstrofes")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido materno solo debe contener letras, acentos, diéresis, guiones o apóstrofes")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[a-zA-Z]{4}\\d{6}[hHmM][a-zA-Z]{5}[a-zA-Z0-9]\\d$", message = "La CURP debe tener un formato oficial válido de 18 caracteres")
    private String curp;

    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[a-zA-ZñÑ&]{4}\\d{6}[a-zA-Z0-9]{3}$", message = "El RFC para persona física debe cumplir el formato oficial AAAA000000XXX (13 caracteres)")
    @Size(min = 13, max = 13, message = "El RFC para persona física debe tener exactamente 13 caracteres")
    private String rfc;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "^[hmxHMX]$", message = "El sexo debe ser 'H' (Hombre), 'M' (Mujer) o 'X' (No binario)")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(min = 2, max = 50, message = "La nacionalidad debe tener entre 2 y 50 caracteres")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    @Size(min = 2, max = 50, message = "El estado civil debe tener entre 2 y 50 caracteres")
    private String estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "El correo electrónico debe tener un formato válido (ej. usuario@dominio.com)")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    private String email;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil es inválido: solo debe contener exactamente 10 dígitos numéricos (sin letras, espacios ni caracteres especiales)")
    private String telefonoMovil;

    @Pattern(regexp = "^(\\d{10})?$", message = "El teléfono alternativo es inválido: solo debe contener exactamente 10 dígitos numéricos (sin letras, espacios ni caracteres especiales)")
    private String telefonoAlternativo;

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(min = 2, max = 100, message = "La ocupación debe tener entre 2 y 100 caracteres")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(min = 2, max = 100, message = "La empresa debe tener entre 2 y 100 caracteres")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;
}
