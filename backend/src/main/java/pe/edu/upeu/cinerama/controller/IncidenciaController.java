package pe.edu.upeu.cinerama.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.IncidenciaDTO;
import pe.edu.upeu.cinerama.dto.IncidenciaRequestDTO;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.security.IdentidadActual;
import pe.edu.upeu.cinerama.services.IIncidenciaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidencias")
@SecurityRequirement(name = "bearerAuth")
public class IncidenciaController {

    private final IIncidenciaService incidenciaService;

    public IncidenciaController(IIncidenciaService incidenciaService) {
        this.incidenciaService = incidenciaService;
    }

    @GetMapping
    public ResponseEntity<List<IncidenciaDTO>> listar() {
        return ResponseEntity.ok(incidenciaService.listarTodo());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<IncidenciaDTO>> listarPendientes() {
        return ResponseEntity.ok(incidenciaService.listarPendientes());
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<IncidenciaDTO>> listarPorCliente(@PathVariable Long idCliente) {
        return ResponseEntity.ok(incidenciaService.listarPorCliente(idCliente));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<IncidenciaDTO>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(incidenciaService.listarPorEstado(estado));
    }

    @GetMapping("/mias")
    public ResponseEntity<List<IncidenciaDTO>> listarMias(HttpServletRequest request) {
        rechazarAdmin(request);
        Long idCliente = IdentidadActual.idCliente(request);
        return ResponseEntity.ok(incidenciaService.listarPorCliente(idCliente));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidenciaDTO> obtener(@PathVariable Long id) {
        return incidenciaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada con id: " + id));
    }

    @PostMapping
    public ResponseEntity<IncidenciaDTO> crear(@Valid @RequestBody IncidenciaRequestDTO dto,
                                               HttpServletRequest request) {
        rechazarAdmin(request);
        Long idCliente = IdentidadActual.idCliente(request);
        return new ResponseEntity<>(incidenciaService.crear(dto, idCliente), HttpStatus.CREATED);
    }

    /** El administrador atiende el soporte, así que no registra incidencias como cliente. */
    private void rechazarAdmin(HttpServletRequest request) {
        if (IdentidadActual.esAdmin(request)) {
            throw new BadRequestException(
                    "El administrador gestiona el soporte y no puede registrar incidencias");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidenciaDTO> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody IncidenciaRequestDTO dto) {
        return incidenciaService.actualizar(id, dto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo actualizar con id: " + id));
    }

    @PatchMapping("/{id}/estado/{estado}")
    public ResponseEntity<IncidenciaDTO> cambiarEstado(@PathVariable Long id,
                                                       @PathVariable String estado) {
        return ResponseEntity.ok(incidenciaService.cambiarEstado(id, estado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (incidenciaService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }
}
