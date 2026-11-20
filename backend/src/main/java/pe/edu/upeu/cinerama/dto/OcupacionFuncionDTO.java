package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OcupacionFuncionDTO(
        Long idFuncion,
        String titulo,
        String sala,
        LocalDateTime horaInicio,
        int capacidad,
        int compras,
        int vendidas,
        BigDecimal ocupacionPct
) {
}
