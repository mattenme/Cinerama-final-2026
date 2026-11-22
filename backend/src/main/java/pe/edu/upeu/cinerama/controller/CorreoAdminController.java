package pe.edu.upeu.cinerama.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.cinerama.dto.CorreoEnviadoDTO;
import pe.edu.upeu.cinerama.dto.PruebaCorreoDTO;
import pe.edu.upeu.cinerama.dto.PruebaCorreoRequestDTO;
import pe.edu.upeu.cinerama.services.BandejaCorreoService;
import pe.edu.upeu.cinerama.services.IEmailService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/correo")
@SecurityRequirement(name = "bearerAuth")
public class CorreoAdminController {

    private final IEmailService emailService;
    private final BandejaCorreoService bandeja;

    public CorreoAdminController(IEmailService emailService, BandejaCorreoService bandeja) {
        this.emailService = emailService;
        this.bandeja = bandeja;
    }

    @PostMapping("/prueba")
    public ResponseEntity<PruebaCorreoDTO> probar(@Valid @RequestBody PruebaCorreoRequestDTO dto) {
        return ResponseEntity.ok(emailService.probarEnvio(dto.destino().trim()));
    }

    @GetMapping("/bandeja")
    public ResponseEntity<List<CorreoEnviadoDTO>> bandeja() {
        return ResponseEntity.ok(bandeja.listar());
    }

    @DeleteMapping("/bandeja")
    public ResponseEntity<Map<String, Integer>> limpiarBandeja() {
        return ResponseEntity.ok(Map.of("eliminados", bandeja.limpiar()));
    }
}
