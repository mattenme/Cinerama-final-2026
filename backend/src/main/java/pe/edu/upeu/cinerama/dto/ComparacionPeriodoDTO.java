package pe.edu.upeu.cinerama.dto;

import java.math.BigDecimal;

public record ComparacionPeriodoDTO(
        String metrica,
        BigDecimal actual,
        BigDecimal anterior,
        BigDecimal variacion,
        BigDecimal variacionPct
) {
}
