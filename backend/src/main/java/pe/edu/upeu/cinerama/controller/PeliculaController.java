package pe.edu.upeu.cinerama.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.cinerama.dto.PeliculaDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.services.IPeliculaService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/peliculas")
public class PeliculaController {

    private final IPeliculaService peliculaService;

    public PeliculaController(IPeliculaService peliculaService) {
        this.peliculaService = peliculaService;
    }

    @GetMapping
    public ResponseEntity<List<PeliculaDTO>> listar() {
        return ResponseEntity.ok(peliculaService.listarTodo());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<PeliculaDTO>> listarActivas() {
        return ResponseEntity.ok(peliculaService.listarActivas());
    }

    @GetMapping("/destacadas")
    public ResponseEntity<List<PeliculaDTO>> listarDestacadas() {
        return ResponseEntity.ok(peliculaService.listarDestacadas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeliculaDTO> obtener(@PathVariable Long id) {
        return peliculaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con id: " + id));
    }

    @PostMapping
    public ResponseEntity<PeliculaDTO> crear(@Valid @RequestBody PeliculaDTO dto) {
        return new ResponseEntity<>(peliculaService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PeliculaDTO> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody PeliculaDTO dto) {
        return peliculaService.actualizar(id, dto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo actualizar con id: " + id));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<PeliculaDTO> cambiarActivo(@PathVariable Long id) {
        return ResponseEntity.ok(peliculaService.cambiarActivo(id));
    }

    @PatchMapping("/{id}/destacado")
    public ResponseEntity<PeliculaDTO> cambiarDestacado(@PathVariable Long id) {
        return ResponseEntity.ok(peliculaService.cambiarDestacado(id));
    }

    @PostMapping("/{id}/imagen")
    public ResponseEntity<Map<String, String>> subirImagen(@PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file) throws IOException {
        String imagenUrl = peliculaService.subirImagen(id, file);
        return ResponseEntity.ok(Map.of("imagenUrl", imagenUrl));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (peliculaService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }
}
