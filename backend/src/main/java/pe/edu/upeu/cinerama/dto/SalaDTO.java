package pe.edu.upeu.cinerama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SalaDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long idSala,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String tipo,
        Integer capacidadTotal,
        Integer activo
) {
}
