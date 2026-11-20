package pe.edu.upeu.cinerama.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.cinerama.dto.ProductoDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.services.IProductoService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final IProductoService productoService;

    public ProductoController(IProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listar() {
        return ResponseEntity.ok(productoService.listarTodo());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ProductoDTO>> listarActivos() {
        return ResponseEntity.ok(productoService.listarActivos());
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<String>> listarCategorias() {
        return ResponseEntity.ok(productoService.listarCategorias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> obtener(@PathVariable Long id) {
        return productoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    @PostMapping
    public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoDTO dto) {
        return new ResponseEntity<>(productoService.crear(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody ProductoDTO dto) {
        return productoService.actualizar(id, dto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo actualizar con id: " + id));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<ProductoDTO> cambiarActivo(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.cambiarActivo(id));
    }

    @PostMapping("/{id}/imagen")
    public ResponseEntity<Map<String, String>> subirImagen(@PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file) throws IOException {
        String imagenUrl = productoService.subirImagen(id, file);
        return ResponseEntity.ok(Map.of("imagenUrl", imagenUrl));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (productoService.eliminar(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar con id: " + id);
    }
}
