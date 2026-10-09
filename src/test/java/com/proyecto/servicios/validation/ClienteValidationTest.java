package com.proyecto.servicios.validation;

import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.DomicilioDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pruebas Exhaustivas de Validación de Restricciones y Reglas de Negocio")
class ClienteValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private ClienteRegistroRequest crearRequestValido() {
        return ClienteRegistroRequest.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Lopez")
                .apellidoMaterno("Garcia")
                .fechaNacimiento(LocalDate.of(1990, 5, 15))
                .curp("LOGJ900515HDFRRN01")
                .rfc("LOGJ900515AB1")
                .sexo("H")
                .nacionalidad("Mexicana")
                .estadoCivil("Soltero")
                .email("juan.lopez@example.com")
                .telefonoMovil("5512345678")
                .telefonoAlternativo("5587654321")
                .ocupacion("Desarrollador")
                .empresa("Tech Solutions")
                .ingresoMensual(new BigDecimal("35000.00"))
                .password("Password123!")
                .domicilio(DomicilioDTO.builder()
                        .calle("Av. Reforma")
                        .numeroExterior("123")
                        .numeroInterior("A-10")
                        .colonia("Juarez")
                        .municipio("Cuauhtemoc")
                        .estado("Ciudad de Mexico")
                        .codigoPostal("06600")
                        .pais("Mexico")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Request completamente válido no debe producir violaciones")
    void validacion_RequestValido_SinViolaciones() {
        ClienteRegistroRequest request = crearRequestValido();
        Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Un request con datos válidos no debe generar violaciones");
    }

    @Nested
    @DisplayName("1. Validaciones de RFC (Formato AAAA000000XXX y longitud exacta 13)")
    class ValidacionesRfc {

        @ParameterizedTest
        @ValueSource(strings = {
                "ABCD900101AB",       // 12 caracteres (menos de 13)
                "ABCD900101ABCD1",    // 14 caracteres (más de 13)
                "ABCD900101-01",      // con guion
                "ABCD900101.01",      // con punto
                "ABC900101AB1",       // solo 3 letras iniciales
                "ABCDE00101AB1",      // 5 letras iniciales
                "ABCD900101A",        // sin homoclave completa
                "1234900101AB1",      // números en iniciales
                ""                    // vacío
        })
        @DisplayName("RFC con formato o longitud incorrecta debe ser rechazado")
        void rfcInvalido_ProduceViolacion(String rfcInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setRfc(rfcInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "RFC inválido '" + rfcInvalido + "' debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rfc")));
        }

        @Test
        @DisplayName("RFC en minúsculas es aceptado para sanitización automática a mayúsculas")
        void rfcMinusculas_EsValidoParaSanitizacion() {
            ClienteRegistroRequest request = crearRequestValido();
            request.setRfc("pelj920814ab1");

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "RFC en minúsculas debe ser válido para auto-corrección");
        }
    }

    @Nested
    @DisplayName("2. Validaciones de CURP (Formato oficial 18 caracteres)")
    class ValidacionesCurp {

        @ParameterizedTest
        @ValueSource(strings = {
                "LOGJ900515HDFRRN0",     // 17 caracteres (menos de 18)
                "LOGJ900515HDFRRN012",   // 19 caracteres (más de 18)
                "LOGJ900515ZDFRRN01",   // sexo inválido 'Z'
                "LOGJ900515HDFRRN-1",   // con guión
                "1234900515HDFRRN01",   // números en iniciales
                ""                      // vacío
        })
        @DisplayName("CURP inválida debe ser rechazada")
        void curpInvalida_ProduceViolacion(String curpInvalida) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setCurp(curpInvalida);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "CURP inválida '" + curpInvalida + "' debió ser rechazada");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("curp")));
        }

        @Test
        @DisplayName("CURP en minúsculas es aceptada para sanitización automática a mayúsculas")
        void curpMinusculas_EsValidaParaSanitizacion() {
            ClienteRegistroRequest request = crearRequestValido();
            request.setCurp("logj900515hdfrrn01");

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "CURP en minúsculas debe ser válida para auto-corrección");
        }
    }

    @Nested
    @DisplayName("3. Validaciones de Nombres y Apellidos (Solo letras, sin números ni símbolos)")
    class ValidacionesNombres {

        @ParameterizedTest
        @ValueSource(strings = {
                "Juan123",       // contiene números
                "Juan.",         // contiene punto
                "Juan, Carlos",  // contiene coma
                "Juan@Perez",    // contiene arroba
                "Juan#1",        // contiene numeral
                "J",             // longitud menor a 2 caracteres
                ""               // vacío
        })
        @DisplayName("Nombre inválido debe ser rechazado")
        void nombreInvalido_ProduceViolacion(String nombreInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setNombre(nombreInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Nombre inválido '" + nombreInvalido + "' debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("nombre")));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Lopez123",
                "Lopez.",
                "Lopez, Garcia",
                "L",
                ""
        })
        @DisplayName("Apellido paterno inválido debe ser rechazado")
        void apellidoPaternoInvalido_ProduceViolacion(String apellidoInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setApellidoPaterno(apellidoInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Apellido paterno '" + apellidoInvalido + "' debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("apellidoPaterno")));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Garcia1",
                "Garcia*",
                "G",
                ""
        })
        @DisplayName("Apellido materno inválido debe ser rechazado")
        void apellidoMaternoInvalido_ProduceViolacion(String apellidoInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setApellidoMaterno(apellidoInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Apellido materno '" + apellidoInvalido + "' debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("apellidoMaterno")));
        }
    }

    @Nested
    @DisplayName("4. Validaciones de Teléfono (Exactamente 10 dígitos numéricos)")
    class ValidacionesTelefono {

        @ParameterizedTest
        @ValueSource(strings = {
                "551234567",       // 9 dígitos (menos de 10)
                "55123456789",     // 11 dígitos (más de 10)
                "551234ABCD",      // contiene letras
                "55-1234-56",      // contiene guiones
                "(55)123456",      // contiene paréntesis
                "55 1234 56",      // contiene espacios
                ""                 // vacío
        })
        @DisplayName("Teléfono móvil inválido debe ser rechazado")
        void telefonoMovilInvalido_ProduceViolacion(String telefonoInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setTelefonoMovil(telefonoInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Teléfono inválido '" + telefonoInvalido + "' debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("telefonoMovil")));
        }
    }

    @Nested
    @DisplayName("5. Validaciones de Correo Electrónico")
    class ValidacionesEmail {

        @ParameterizedTest
        @ValueSource(strings = {
                "correo_sin_arroba.com",
                "correo@",
                "@dominio.com",
                "correo@dominio",
                ""
        })
        @DisplayName("Correo electrónico inválido debe ser rechazado")
        void emailInvalido_ProduceViolacion(String emailInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setEmail(emailInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Email inválido '" + emailInvalido + "' debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        }
    }

    @Nested
    @DisplayName("6. Validaciones de Fecha de Nacimiento y Mayoría de Edad")
    class ValidacionesFechaNacimiento {

        @Test
        @DisplayName("Fecha de nacimiento futura debe ser rechazada")
        void fechaNacimientoFutura_ProduceViolacion() {
            ClienteRegistroRequest request = crearRequestValido();
            request.setFechaNacimiento(LocalDate.now().plusDays(1));

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Fecha futura debió ser rechazada");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("fechaNacimiento")));
        }
    }

    @Nested
    @DisplayName("7. Validaciones de Domicilio y Código Postal (Exactamente 5 dígitos)")
    class ValidacionesDomicilio {

        @ParameterizedTest
        @ValueSource(strings = {
                "1234",      // 4 dígitos (menos de 5)
                "123456",    // 6 dígitos (más de 5)
                "0660A",     // contiene letra
                "06-60",     // contiene guion
                ""           // vacío
        })
        @DisplayName("Código postal inválido debe ser rechazado")
        void codigoPostalInvalido_ProduceViolacion(String cpInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.getDomicilio().setCodigoPostal(cpInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Código postal '" + cpInvalido + "' debió ser rechazado");
        }
    }

    @Nested
    @DisplayName("8. Validaciones de Ingreso Mensual (Mayor a 0)")
    class ValidacionesIngreso {

        @Test
        @DisplayName("Ingreso mensual igual a cero o negativo debe ser rechazado")
        void ingresoMensualCeroONegativo_ProduceViolacion() {
            ClienteRegistroRequest request = crearRequestValido();
            request.setIngresoMensual(BigDecimal.ZERO);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Ingreso mensual 0 debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("ingresoMensual")));

            request.setIngresoMensual(new BigDecimal("-100.00"));
            violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Ingreso mensual negativo debió ser rechazado");
        }
    }

    @Nested
    @DisplayName("9. Validaciones de Contraseña (Mayúscula, minúscula, número, especial, >= 8 chars)")
    class ValidacionesPassword {

        @ParameterizedTest
        @ValueSource(strings = {
                "password123!",   // sin mayúscula
                "PASSWORD123!",   // sin minúscula
                "Password!!!",    // sin número
                "Password123",    // sin carácter especial
                "Pass1!",         // menor a 8 caracteres
                ""                // vacía
        })
        @DisplayName("Contraseña que no cumpla los criterios debe ser rechazada")
        void passwordInvalida_ProduceViolacion(String passwordInvalida) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setPassword(passwordInvalida);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Contraseña '" + passwordInvalida + "' debió ser rechazada");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }
    }

    @Nested
    @DisplayName("10. Validaciones de Sexo ('H', 'M', 'X')")
    class ValidacionesSexo {

        @ParameterizedTest
        @ValueSource(strings = {"Z", "F", "1", "Hombre", "Mujer", ""})
        @DisplayName("Sexo inválido debe ser rechazado")
        void sexoInvalido_ProduceViolacion(String sexoInvalido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setSexo(sexoInvalido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty(), "Sexo inválido '" + sexoInvalido + "' debió ser rechazado");
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("sexo")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"h", "m", "x", "H", "M", "X"})
        @DisplayName("Sexo en mayúsculas o minúsculas es aceptado para sanitización")
        void sexoValido_EsAceptado(String sexoValido) {
            ClienteRegistroRequest request = crearRequestValido();
            request.setSexo(sexoValido);

            Set<ConstraintViolation<ClienteRegistroRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Sexo válido '" + sexoValido + "' debe ser aceptado");
        }
    }
}
