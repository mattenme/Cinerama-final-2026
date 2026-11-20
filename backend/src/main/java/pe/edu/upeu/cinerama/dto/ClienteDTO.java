package pe.edu.upeu.cinerama.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClienteDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long idCliente,

        String dni,
        String nombre,
        String email,
        String telefono,
        String rol,
        Integer activo,
        Integer verificado
) {
}
