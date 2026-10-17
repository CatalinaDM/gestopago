package com.proyecto.servicios.security;

import com.proyecto.servicios.controller.ClienteController;
import com.proyecto.servicios.controller.CuentaController;
import com.proyecto.servicios.controller.UsuarioController;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.model.usuario.ActualizarPasswordRequest;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Seguridad: Control de Acceso basado en Roles (RBAC) y Prevención IDOR")
class RbacSecurityTest {

    @Mock
    private ClienteService clienteService;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private ClienteController clienteController;

    @InjectMocks
    private CuentaController cuentaController;

    @InjectMocks
    private UsuarioController usuarioController;

    @Nested
    @DisplayName("1. Control de Acceso en Clientes (/clientes)")
    class ClientesSecurity {

        @Test
        @DisplayName("Cliente no puede consultar clientes -> 403 Forbidden")
        void clienteNoPuedeConsultarClientes() {
            com.proyecto.servicios.model.GenericRequest<com.proyecto.servicios.model.cliente.ClienteConsultaRequest> req = new com.proyecto.servicios.model.GenericRequest<>();
            assertThrows(AccesoDenegadoException.class, () ->
                    clienteController.consultarClientes(2, req));
        }

        @Test
        @DisplayName("Cliente no puede dar de baja clientes -> 403 Forbidden")
        void clienteNoPuedeDarDeBaja() {
            com.proyecto.servicios.model.GenericRequest<com.proyecto.servicios.model.cliente.ClienteAccionRequest> req = new com.proyecto.servicios.model.GenericRequest<>();
            req.setDatos(new com.proyecto.servicios.model.cliente.ClienteAccionRequest(99));

            assertThrows(AccesoDenegadoException.class, () ->
                    clienteController.darDeBajaCliente(req, 2));
        }

        @Test
        @DisplayName("Cliente no puede reactivar clientes -> 403 Forbidden")
        void clienteNoPuedeReactivar() {
            com.proyecto.servicios.model.GenericRequest<com.proyecto.servicios.model.cliente.ClienteAccionRequest> req = new com.proyecto.servicios.model.GenericRequest<>();
            req.setDatos(new com.proyecto.servicios.model.cliente.ClienteAccionRequest(99));

            assertThrows(AccesoDenegadoException.class, () ->
                    clienteController.reactivarCliente(req, 2));
        }

        @Test
        @DisplayName("Administrador sí puede consultar clientes")
        void adminPuedeConsultarClientes() {
            com.proyecto.servicios.model.GenericRequest<com.proyecto.servicios.model.cliente.ClienteConsultaRequest> req = new com.proyecto.servicios.model.GenericRequest<>();
            clienteController.consultarClientes(1, req);
            verify(clienteService).consultar(req);
        }

        @Test
        @DisplayName("Administrador sí puede reactivar clientes")
        void adminPuedeReactivar() {
            com.proyecto.servicios.model.GenericRequest<com.proyecto.servicios.model.cliente.ClienteAccionRequest> req = new com.proyecto.servicios.model.GenericRequest<>();
            req.setDatos(new com.proyecto.servicios.model.cliente.ClienteAccionRequest(99));

            clienteController.reactivarCliente(req, 1);
            verify(clienteService).reactivarCliente(req);
        }

        @Test
        @DisplayName("Cliente puede consultar su propio perfil en /clientes/me")
        void clientePuedeConsultarSuPerfil() {
            clienteController.obtenerMiPerfil(5);
            verify(clienteService).obtenerPerfil(5);
        }
    }

    @Nested
    @DisplayName("2. Control de Acceso en Cuentas (/cuentas)")
    class CuentasSecurity {

        @Test
        @DisplayName("Cliente no puede listar todas las cuentas activas -> 403 Forbidden")
        void clienteNoPuedeListarCuentasActivas() {
            assertThrows(AccesoDenegadoException.class, () ->
                    cuentaController.obtenerCuentasActivas(2));
        }

        @Test
        @DisplayName("Administrador sí puede listar todas las cuentas activas")
        void adminPuedeListarCuentasActivas() {
            cuentaController.obtenerCuentasActivas(1);
            verify(cuentaService).obtenerCuentasActivas();
        }

        @Test
        @DisplayName("Cliente puede consultar sus propias cuentas en /cuentas/me")
        void clientePuedeConsultarSusCuentas() {
            cuentaController.obtenerMisCuentas(5);
            verify(cuentaService).obtenerCuentasPorCliente(5);
        }
    }

    @Nested
    @DisplayName("3. Control de Acceso en Usuarios (/usuarios) y Prevención IDOR")
    class UsuariosSecurity {

        @Test
        @DisplayName("Cliente no puede consultar detalles de usuarios por ID -> 403 Forbidden")
        void clienteNoPuedeConsultarUsuarioPorId() {
            com.proyecto.servicios.model.GenericRequest<com.proyecto.servicios.model.usuario.UsuarioConsultaRequest> req = new com.proyecto.servicios.model.GenericRequest<>();
            req.setDatos(new com.proyecto.servicios.model.usuario.UsuarioConsultaRequest(10));

            assertThrows(AccesoDenegadoException.class, () ->
                    usuarioController.obtenerPorId(req, 2));
        }

        @Test
        @DisplayName("Cliente no puede desbloquear usuarios -> 403 Forbidden")
        void clienteNoPuedeDesbloquearUsuario() {
            com.proyecto.servicios.model.GenericRequest<com.proyecto.servicios.model.usuario.UsuarioDesbloqueoRequest> req = new com.proyecto.servicios.model.GenericRequest<>();
            req.setDatos(new com.proyecto.servicios.model.usuario.UsuarioDesbloqueoRequest(10));

            assertThrows(AccesoDenegadoException.class, () ->
                    usuarioController.desbloquearUsuario(req, 2));
        }

        @Test
        @DisplayName("Verificar compatibilidad de hash BCrypt para admin")
        void verificarHashBcrypt() {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            String hash = "$2a$10$0p8g6E/btl48HAmaHdI9/OF8CIJkujmfD.6caW6X.3YrPbZSWEiEW";
            org.junit.jupiter.api.Assertions.assertTrue(encoder.matches("Admin123!", hash));
        }

        @Test
        @DisplayName("Usuario no puede cambiar la contraseña de otro usuario (IDOR) -> 403 Forbidden")
        void usuarioNoPuedeCambiarPasswordDeOtro() {
            ActualizarPasswordRequest req = ActualizarPasswordRequest.builder()
                    .id(10)
                    .passwordActual("PassActual1!")
                    .passwordNuevo("PassNuevo1!")
                    .build();
            com.proyecto.servicios.model.GenericRequest<ActualizarPasswordRequest> genericReq = new com.proyecto.servicios.model.GenericRequest<>();
            genericReq.setDatos(req);

            // Usuario con ID 5 intenta cambiar el password del usuario con ID 10
            assertThrows(AccesoDenegadoException.class, () ->
                    usuarioController.actualizarPassword(genericReq, null, 5));
        }

        @Test
        @DisplayName("Usuario autenticado sí puede cambiar su propia contraseña")
        void usuarioPuedeCambiarSuPropiaPassword() {
            ActualizarPasswordRequest req = ActualizarPasswordRequest.builder()
                    .id(5)
                    .passwordActual("PassActual1!")
                    .passwordNuevo("PassNuevo1!")
                    .build();
            com.proyecto.servicios.model.GenericRequest<ActualizarPasswordRequest> genericReq = new com.proyecto.servicios.model.GenericRequest<>();
            genericReq.setDatos(req);

            // Usuario con ID 5 cambia su propio password (ID 5)
            usuarioController.actualizarPassword(genericReq, null, 5);
            verify(usuarioService).actualizarPassword(genericReq);
        }
    }
}
