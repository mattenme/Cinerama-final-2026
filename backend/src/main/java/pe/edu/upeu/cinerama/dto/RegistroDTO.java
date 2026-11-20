package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroDTO(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener exactamente 8 dígitos")
        String dni,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        @Size(max = 150, message = "El email no puede exceder 150 caracteres")
        String email,

        @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
        String telefono,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 255, message = "La contraseña debe tener al menos 6 caracteres")
        String contrasena
) {
}
