package pe.edu.upeu.cinerama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AsientoDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        Long idAsiento,
        Long idSala,
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        String nombreSala,
        String fila,
        Integer numero,
        String estado
) {
}
