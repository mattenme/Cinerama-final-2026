package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;

public record ClienteVsPromedioDTO(
        Long idCliente,
        String dni,
        String nombre,
        int compras,
        BigDecimal totalCliente,
        BigDecimal promedio,
        BigDecimal diferencia,
        BigDecimal porcentaje,
        Integer posicion,
        int clientes
) {
}
