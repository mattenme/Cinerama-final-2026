package pe.edu.upeu.cinerama.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemDulceriaDTO(
        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad
) {
}
