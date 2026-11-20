package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidacionRequestDTO(
        @NotBlank(message = "El código QR es obligatorio")
        String codigo
) {
}
