package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraDTO(
        Long idCompra,
        Long idCliente,
        String nombreCliente,
        Long idFuncion,
        String tituloPelicula,
        String nombreSala,
        LocalDateTime horaInicio,
        Long idPelicula,
        LocalDateTime finFuncion,
        Long idAsiento,
        String filaAsiento,
        Integer numeroAsiento,
        BigDecimal precio,
        BigDecimal totalProductos,
        String metodoPago,
        String estado,
        String codigoQr,
        LocalDateTime fechaCompra,
        String productos,
        Integer usado,
        LocalDateTime fechaUso,
        LocalDateTime fechaCancelacion
) {
}
