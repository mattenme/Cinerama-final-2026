package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenvioCodigoDTO(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email
) {
}
