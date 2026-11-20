package pe.edu.upeu.cinerama.dto;

public record AuthResponseDTO(
        boolean success,
        String message,
        String rol,
        Long clienteId,
        String nombre,
        String codigoDesarrollo,

        String token,

        Long expiraEnSegundos
) {
}
