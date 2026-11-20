package pe.edu.upeu.cinerama.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record IncidenciaDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long idIncidencia,

        @NotBlank(message = "El tipo de incidencia es obligatorio")
        String tipo,

        String descripcion,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        LocalDateTime fechaReporte,

        String estado,

        Long idSala,
        Long idFuncion,
        Long idCliente,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        String nombreCliente,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        String nombreSala,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        String tituloPelicula
) {
}
