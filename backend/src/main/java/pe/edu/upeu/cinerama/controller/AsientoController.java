package pe.edu.upeu.cinerama.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.AsientoDTO;
import pe.edu.upeu.cinerama.dto.GenerarAsientosDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.services.IAsientoService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/asientos")
public class AsientoController {

    private final IAsientoService asientoService;

    public AsientoController(IAsientoService asientoService) {
        this.asientoService = asientoService;
    }

    @GetMapping
    public ResponseEntity<List<AsientoDTO>> listar() {
        return ResponseEntity.ok(asientoService.listarTodo());
    }

    @GetMapping("/sala/{idSala}")
    public ResponseEntity<List<AsientoDTO>> listarPorSala(@PathVariable Long idSala) {
        return ResponseEntity.ok(asientoService.listarPorSala(idSala));
    }

    @GetMapping("/sala/{idSala}/disponibles")
    public ResponseEntity<List<AsientoDTO>> listarDisponiblesPorSala(@PathVariable Long idSala) {
        return ResponseEntity.ok(asientoService.listarDisponiblesPorSala(idSala));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AsientoDTO> obtener(@PathVariable Long id) {
        return asientoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Asiento no encontrado con id: " + id));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<AsientoDTO> crear(@Valid @RequestBody AsientoDTO dto) {
        return new ResponseEntity<>(asientoService.crear(dto), HttpStatus.CREATED);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/sala/{idSala}/generar")
    public ResponseEntity<List<AsientoDTO>> generarButacas(@PathVariable Long idSala,
                                                           @Valid @RequestBody GenerarAsientosDTO dto) {
        return new ResponseEntity<>(
                asientoService.generarButacas(idSala, dto.filas(), dto.butacasPorFila()),
                HttpStatus.CREATED);
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (asientoService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }
}
