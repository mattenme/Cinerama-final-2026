package pe.edu.upeu.cinerama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PeliculaDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long idPelicula,

        @NotBlank(message = "El título es obligatorio")
        String titulo,

        @NotNull(message = "La duración es obligatoria")
        @Positive(message = "La duración debe ser mayor a cero")
        Integer duracionMinutos,

        String genero,
        String sinopsis,
        String imagenUrl,
        Integer destacado,
        Integer activo
) {
}
