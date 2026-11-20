package pe.edu.upeu.cinerama.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.FuncionDTO;
import pe.edu.upeu.cinerama.dto.FuncionRequestDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.services.IFuncionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/funciones")
public class FuncionController {

    private final IFuncionService funcionService;

    public FuncionController(IFuncionService funcionService) {
        this.funcionService = funcionService;
    }

    @GetMapping
    public ResponseEntity<List<FuncionDTO>> listar() {
        return ResponseEntity.ok(funcionService.listarTodo());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<FuncionDTO>> listarActivas() {
        return ResponseEntity.ok(funcionService.listarActivas());
    }

    @GetMapping("/pelicula/{idPelicula}")
    public ResponseEntity<List<FuncionDTO>> listarPorPelicula(@PathVariable Long idPelicula) {
        return ResponseEntity.ok(funcionService.listarPorPelicula(idPelicula));
    }

    @GetMapping("/sala/{idSala}")
    public ResponseEntity<List<FuncionDTO>> listarPorSala(@PathVariable Long idSala) {
        return ResponseEntity.ok(funcionService.listarPorSala(idSala));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuncionDTO> obtener(@PathVariable Long id) {
        return funcionService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con id: " + id));
    }

    @PostMapping
    public ResponseEntity<FuncionDTO> crear(@Valid @RequestBody FuncionRequestDTO dto) {
        return new ResponseEntity<>(funcionService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FuncionDTO> actualizar(@PathVariable Long id,
                                                 @Valid @RequestBody FuncionRequestDTO dto) {
        return funcionService.actualizar(id, dto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo actualizar con id: " + id));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<FuncionDTO> cambiarActivo(@PathVariable Long id) {
        return ResponseEntity.ok(funcionService.cambiarActivo(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (funcionService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }
}
