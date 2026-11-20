package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;

public record VentaPeliculaDTO(
        Long idPelicula,
        String titulo,
        int compras,
        int butacas,
        BigDecimal entradas,
        BigDecimal dulceria,
        BigDecimal total
) {
}
