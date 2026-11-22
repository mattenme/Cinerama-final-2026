package pe.edu.upeu.cinerama.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.ClienteDTO;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.security.IdentidadActual;
import pe.edu.upeu.cinerama.services.IClienteService;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/clientes")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final IClienteService clienteService;

    public ClienteController(IClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteDTO>> listar() {
        return ResponseEntity.ok(clienteService.listarTodo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> obtener(@PathVariable Long id) {
        return clienteService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<ClienteDTO> cambiarActivo(@PathVariable Long id,
                                                    HttpServletRequest request) {
        impedirCambioPropio(id, request, "desactivar");
        return ResponseEntity.ok(clienteService.cambiarActivo(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, HttpServletRequest request) {
        impedirCambioPropio(id, request, "eliminar");
        if (clienteService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }

    /** Un administrador no puede desactivar ni eliminar su propia cuenta. */
    private void impedirCambioPropio(Long id, HttpServletRequest request, String accion) {
        if (Objects.equals(IdentidadActual.idCliente(request), id)) {
            throw new BadRequestException(
                    "No puedes " + accion + " tu propia cuenta de administrador");
        }
    }
}
