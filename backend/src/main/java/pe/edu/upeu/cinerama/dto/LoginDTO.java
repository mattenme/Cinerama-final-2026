package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "El DNI es obligatorio")
        String dni,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}
