package pe.edu.upeu.cinerama.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.CalificacionDTO;
import pe.edu.upeu.cinerama.dto.CalificacionRequestDTO;
import pe.edu.upeu.cinerama.dto.CalificacionPromedioDTO;
import pe.edu.upeu.cinerama.dto.PuedeCalificarDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.security.IdentidadActual;
import pe.edu.upeu.cinerama.services.ICalificacionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calificaciones")
public class CalificacionController {

    private final ICalificacionService calificacionService;

    public CalificacionController(ICalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    @GetMapping
    public ResponseEntity<List<CalificacionDTO>> listar() {
        return ResponseEntity.ok(calificacionService.listarTodo());
    }

    @GetMapping("/pelicula/{idPelicula}")
    public ResponseEntity<List<CalificacionDTO>> listarPorPelicula(@PathVariable Long idPelicula) {
        return ResponseEntity.ok(calificacionService.listarPorPelicula(idPelicula));
    }

    @GetMapping("/pelicula/{idPelicula}/mia")
    public ResponseEntity<CalificacionDTO> miCalificacion(@PathVariable Long idPelicula,
                                                          HttpServletRequest request) {
        Long idCliente = IdentidadActual.idCliente(request);
        return calificacionService.buscarPorClienteYPelicula(idCliente, idPelicula)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Todavía no calificaste esta película"));
    }

    @GetMapping("/pelicula/{idPelicula}/puede-calificar")
    public ResponseEntity<PuedeCalificarDTO> puedeCalificar(@PathVariable Long idPelicula,
                                                            HttpServletRequest request) {
        Long idCliente = IdentidadActual.idCliente(request);
        return ResponseEntity.ok(calificacionService.elegibilidad(idCliente, idPelicula));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<CalificacionDTO>> listarPorCliente(@PathVariable Long idCliente) {
        return ResponseEntity.ok(calificacionService.listarPorCliente(idCliente));
    }

    @GetMapping("/pelicula/{idPelicula}/promedio")
    public ResponseEntity<CalificacionPromedioDTO> obtenerPromedio(@PathVariable Long idPelicula) {
        Double promedio = calificacionService.promedioPorPelicula(idPelicula);
        Long total = calificacionService.contarPorPelicula(idPelicula);

        return ResponseEntity.ok(new CalificacionPromedioDTO(
                idPelicula,
                promedio == null ? 0 : promedio,
                total == null ? 0 : total
        ));
    }

    @GetMapping("/{idPelicula}/{idCliente}")
    public ResponseEntity<CalificacionDTO> obtener(@PathVariable Long idPelicula,
                                                   @PathVariable Long idCliente) {
        return calificacionService.buscarPorClienteYPelicula(idCliente, idPelicula)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Calificación no encontrada para el cliente " + idCliente));
    }

    @PostMapping
    public ResponseEntity<CalificacionDTO> registrar(@Valid @RequestBody CalificacionRequestDTO dto,
                                                     HttpServletRequest request) {
        Long idCliente = IdentidadActual.idCliente(request);
        return new ResponseEntity<>(calificacionService.registrar(idCliente, dto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{idPelicula}/{idCliente}")
    public ResponseEntity<Void> eliminar(@PathVariable Long idPelicula,
                                         @PathVariable Long idCliente) {
        if (calificacionService.eliminar(idCliente, idPelicula)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("No se pudo eliminar la calificación");
    }
}
