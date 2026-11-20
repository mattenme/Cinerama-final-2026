package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.NotBlank;

public record IncidenciaRequestDTO(
        @NotBlank(message = "El tipo de incidencia es obligatorio")
        String tipo,

        String descripcion,
        Long idSala,
        Long idFuncion,
        String estado
) {
}
