package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerarAsientosDTO(
        @NotNull(message = "El número de filas es obligatorio")
        @Min(value = 1, message = "Debe haber al menos una fila")
        @Max(value = 26, message = "El máximo es 26 filas")
        Integer filas,

        @NotNull(message = "Las butacas por fila son obligatorias")
        @Min(value = 1, message = "Debe haber al menos una butaca por fila")
        @Max(value = 30, message = "El máximo es 30 butacas por fila")
        Integer butacasPorFila
) {
}
