package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;

public record ResumenFuncionDTO(
        Long idFuncion,
        int butacas,
        BigDecimal ingresosEntradas,
        BigDecimal ingresosDulceria,
        BigDecimal total
) {
}
