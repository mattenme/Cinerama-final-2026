package pe.edu.upeu.cinerama.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CalificacionDTO(
        @NotNull(message = "El cliente es obligatorio")
        Long idCliente,

        @NotNull(message = "La película es obligatoria")
        Long idPelicula,

        @NotNull(message = "La puntuación es obligatoria")
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 5, message = "La puntuación máxima es 5")
        Integer puntuacion,

        String comentario,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime fechaCalificacion,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        String nombreCliente
) {
}
