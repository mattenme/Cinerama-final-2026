package pe.edu.upeu.cinerama.dto;

import java.time.LocalDateTime;

public record PruebaCorreoDTO(
        boolean enviado,
        String destino,
        String servidor,
        String remitente,
        String mensaje,
        LocalDateTime fecha
) {
}
