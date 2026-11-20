package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuncionRequestDTO(
        @NotNull(message = "La película es obligatoria")
        Long idPelicula,

        @NotNull(message = "La sala es obligatoria")
        Long idSala,

        @NotNull(message = "La fecha y hora son obligatorias")
        LocalDateTime horaInicio,

        String estado,

        Integer activo,

        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precio
) {
}
