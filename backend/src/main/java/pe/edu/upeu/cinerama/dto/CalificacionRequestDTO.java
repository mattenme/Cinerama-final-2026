package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CalificacionRequestDTO(
        @NotNull(message = "La película es obligatoria")
        Long idPelicula,

        @NotNull(message = "La puntuación es obligatoria")
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 5, message = "La puntuación máxima es 5")
        Integer puntuacion,

        String comentario
) {
}
