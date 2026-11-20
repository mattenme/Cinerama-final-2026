package pe.edu.upeu.cinerama.dto;

import java.time.LocalDateTime;

public record CorreoEnviadoDTO(
        LocalDateTime fecha,
        String destino,
        String asunto,
        boolean enviado,
        String detalle,
        String codigoQr,
        String html
) {
}
