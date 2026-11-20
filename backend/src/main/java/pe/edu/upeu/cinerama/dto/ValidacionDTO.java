package pe.edu.upeu.cinerama.dto;

import java.time.LocalDateTime;

public record ValidacionDTO(
        Long idValidacion,
        Long idCompra,
        String codigoQr,
        String resultado,
        String mensaje,
        LocalDateTime fecha,
        Long idEmpleado,
        String tituloPelicula,
        String nombreSala,
        LocalDateTime horaInicio,
        String asientos,
        String nombreCliente,
        Integer usado,
        LocalDateTime fechaUso
) {
}
