package pe.edu.upeu.cinerama.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompraRequestDTO(
        @NotNull(message = "El ID de la función es obligatorio")
        Long funcionId,

        @NotNull(message = "La lista de asientos es obligatoria")
        @Size(min = 1, message = "Debe seleccionar al menos un asiento")
        List<Long> asientosIds,

        @NotNull(message = "El método de pago es obligatorio")
        String metodoPago,

        String referenciaPago,

        @Valid
        List<ItemDulceriaDTO> productos,

        String codigoPromocion
) {
}
