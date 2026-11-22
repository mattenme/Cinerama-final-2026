package pe.edu.upeu.cinerama.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.SalaDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.services.ISalaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salas")
public class SalaController {

    private final ISalaService salaService;

    public SalaController(ISalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    public ResponseEntity<List<SalaDTO>> listar() {
        return ResponseEntity.ok(salaService.listarTodo());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<SalaDTO>> listarActivas() {
        return ResponseEntity.ok(salaService.listarActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaDTO> obtener(@PathVariable Long id) {
        return salaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada con id: " + id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<SalaDTO> crear(@Valid @RequestBody SalaDTO dto) {
        return new ResponseEntity<>(salaService.crear(dto), HttpStatus.CREATED);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<SalaDTO> actualizar(@PathVariable Long id, @Valid @RequestBody SalaDTO dto) {
        return salaService.actualizar(id, dto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo actualizar con id: " + id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/activo")
    public ResponseEntity<SalaDTO> cambiarActivo(@PathVariable Long id) {
        return ResponseEntity.ok(salaService.cambiarActivo(id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (salaService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }
}
