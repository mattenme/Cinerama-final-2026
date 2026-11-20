package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TopClienteDTO(
        int posicion,
        String dni,
        String nombre,
        int compras,
        int butacas,
        BigDecimal entradas,
        BigDecimal dulceria,
        BigDecimal total,
        BigDecimal ticketPromedio,
        BigDecimal diferenciaPromedio,
        BigDecimal vsPromedioPct,
        LocalDateTime ultimaCompra
) {
}
