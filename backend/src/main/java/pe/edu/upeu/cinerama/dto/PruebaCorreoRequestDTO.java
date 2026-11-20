package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PruebaCorreoRequestDTO(
        @NotBlank(message = "El correo de destino es obligatorio")
        @Email(message = "El correo de destino no es válido")
        String destino
) {
}
