package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuncionDTO(
        Long idFuncion,
        Long idPelicula,
        String tituloPelicula,
        Long idSala,
        String nombreSala,
        LocalDateTime horaInicio,
        String estado,
        Integer activo,
        BigDecimal precio
) {
}
