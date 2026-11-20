package pe.edu.upeu.cinerama.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.ValidacionDTO;
import pe.edu.upeu.cinerama.dto.ValidacionRequestDTO;
import pe.edu.upeu.cinerama.security.IdentidadActual;
import pe.edu.upeu.cinerama.services.IPuertaService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/puerta")
public class PuertaController {

    private final IPuertaService puertaService;

    public PuertaController(IPuertaService puertaService) {
        this.puertaService = puertaService;
    }

    @PostMapping("/validar")
    public ResponseEntity<ValidacionDTO> validar(@Valid @RequestBody ValidacionRequestDTO dto,
                                                 HttpServletRequest request) {
        Long idEmpleado = IdentidadActual.idCliente(request);
        return ResponseEntity.ok(puertaService.validar(dto.codigo(), idEmpleado));
    }

    @GetMapping("/validaciones")
    public ResponseEntity<List<ValidacionDTO>> recientes() {
        return ResponseEntity.ok(puertaService.listarRecientes());
    }
}
