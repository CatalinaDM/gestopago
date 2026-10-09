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
        @DisplayName("Cliente no puede listar todos los clientes -> 403 Forbidden")
        void clienteNoPuedeListarClientes() {
            assertThrows(AccesoDenegadoException.class, () ->
                    clienteController.obtenerClientes(2, null, null, null, null, null));
        }

        @Test
        @DisplayName("Cliente no puede listar clientes paginados -> 403 Forbidden")
        void clienteNoPuedeListarPaginados() {
            assertThrows(AccesoDenegadoException.class, () ->
                    clienteController.obtenerClientesPaginados(2, null, false, Pageable.unpaged()));
        }

        @Test
        @DisplayName("Cliente no puede consultar otro cliente por ID -> 403 Forbidden")
        void clienteNoPuedeConsultarPorId() {
            assertThrows(AccesoDenegadoException.class, () ->
                    clienteController.obtenerPorId(99, 2));
        }

        @Test
        @DisplayName("Cliente no puede dar de baja clientes -> 403 Forbidden")
        void clienteNoPuedeDarDeBaja() {
            assertThrows(AccesoDenegadoException.class, () ->
                    clienteController.darDeBajaCliente(99, 2));
        }

        @Test
        @DisplayName("Administrador sí puede listar todos los clientes")
        void adminPuedeListarClientes() {
            clienteController.obtenerClientes(1, null, null, null, null, null);
            verify(clienteService).obtenerTodos();
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
            assertThrows(AccesoDenegadoException.class, () ->
                    usuarioController.obtenerPorId(10, 2));
        }

        @Test
        @DisplayName("Cliente no puede desbloquear usuarios -> 403 Forbidden")
        void clienteNoPuedeDesbloquearUsuario() {
            assertThrows(AccesoDenegadoException.class, () ->
                    usuarioController.desbloquearUsuario(10, 2));
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
            ActualizarPasswordRequest req = new ActualizarPasswordRequest("PassActual1!", "PassNuevo1!");
            // Usuario con ID 5 intenta cambiar el password del usuario con ID 10
            assertThrows(AccesoDenegadoException.class, () ->
                    usuarioController.actualizarPassword(10, 5, req));
        }

        @Test
        @DisplayName("Usuario autenticado sí puede cambiar su propia contraseña")
        void usuarioPuedeCambiarSuPropiaPassword() {
            ActualizarPasswordRequest req = new ActualizarPasswordRequest("PassActual1!", "PassNuevo1!");
            // Usuario con ID 5 cambia su propio password (ID 5)
            usuarioController.actualizarPassword(5, 5, req);
            verify(usuarioService).actualizarPassword(5, req);
        }
    }
}
