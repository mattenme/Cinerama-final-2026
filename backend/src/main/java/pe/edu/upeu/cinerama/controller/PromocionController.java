package pe.edu.upeu.cinerama.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.PromocionDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.services.IPromocionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promociones")
public class PromocionController {

    private final IPromocionService promocionService;

    public PromocionController(IPromocionService promocionService) {
        this.promocionService = promocionService;
    }

    @GetMapping
    public ResponseEntity<List<PromocionDTO>> listar() {
        return ResponseEntity.ok(promocionService.listarTodo());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<PromocionDTO>> listarActivas() {
        return ResponseEntity.ok(promocionService.listarActivas());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<PromocionDTO> buscarPorCodigo(@PathVariable String codigo) {
        return promocionService.buscarPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con código: " + codigo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocionDTO> obtener(@PathVariable Long id) {
        return promocionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + id));
    }

    @PostMapping
    public ResponseEntity<PromocionDTO> crear(@Valid @RequestBody PromocionDTO dto) {
        return new ResponseEntity<>(promocionService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromocionDTO> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody PromocionDTO dto) {
        return promocionService.actualizar(id, dto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo actualizar con id: " + id));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<PromocionDTO> cambiarActivo(@PathVariable Long id) {
        return ResponseEntity.ok(promocionService.cambiarActivo(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (promocionService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }
}
