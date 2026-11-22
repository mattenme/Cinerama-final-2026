package pe.edu.upeu.cinerama.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.AuthResponseDTO;
import pe.edu.upeu.cinerama.dto.LoginDTO;
import pe.edu.upeu.cinerama.dto.ReenvioCodigoDTO;
import pe.edu.upeu.cinerama.dto.RegistroDTO;
import pe.edu.upeu.cinerama.dto.VerificacionDTO;
import pe.edu.upeu.cinerama.security.IdentidadActual;
import pe.edu.upeu.cinerama.security.JwtService;
import pe.edu.upeu.cinerama.services.IAuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IAuthService authService;
    private final JwtService jwtService;

    public AuthController(IAuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegistroDTO dto) {
        return ResponseEntity.ok(authService.registrar(dto));
    }

    @PostMapping("/verificar")
    public ResponseEntity<AuthResponseDTO> verificar(@Valid @RequestBody VerificacionDTO dto) {
        return ResponseEntity.ok(authService.verificar(dto));
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<AuthResponseDTO> reenviarCodigo(@Valid @RequestBody ReenvioCodigoDTO dto) {
        return ResponseEntity.ok(authService.reenviarCodigo(dto));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/check")
    public ResponseEntity<AuthResponseDTO> check(HttpServletRequest request) {
        Long clienteId = IdentidadActual.idCliente(request);

        if (clienteId == null) {
            return ResponseEntity.ok(new AuthResponseDTO(
                    false, "No hay sesión activa", null, null, null, null, null, 0L));
        }

        String token = IdentidadActual.token(request);
        return ResponseEntity.ok(new AuthResponseDTO(
                true,
                "Sesión activa",
                IdentidadActual.rol(request),
                clienteId,
                IdentidadActual.nombre(request),
                null,
                token,
                jwtService.segundosRestantes(token)
        ));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/renovar")
    public ResponseEntity<AuthResponseDTO> renovar(HttpServletRequest request) {
        String token = IdentidadActual.token(request);
        Long clienteId = IdentidadActual.idCliente(request);

        if (token == null || clienteId == null || !jwtService.isTokenValid(token)) {
            return ResponseEntity.status(401).body(new AuthResponseDTO(
                    false, "La sesión ya expiró, ingresa de nuevo", null, null, null, null, null, 0L));
        }

        return ResponseEntity.ok(new AuthResponseDTO(
                true,
                "Sesión renovada",
                IdentidadActual.rol(request),
                clienteId,
                IdentidadActual.nombre(request),
                null,
                jwtService.renovarToken(token),
                jwtService.expiracionEnSegundos()
        ));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<AuthResponseDTO> logout() {
        return ResponseEntity.ok(new AuthResponseDTO(
                true, "Sesión cerrada correctamente", null, null, null, null, null, 0L));
    }
}
