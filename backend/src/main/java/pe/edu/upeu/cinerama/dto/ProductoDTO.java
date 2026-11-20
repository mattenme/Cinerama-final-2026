package pe.edu.upeu.cinerama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long idProducto,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        BigDecimal precio,

        String imagenUrl,
        String categoria,
        Integer activo,

        @Min(value = 0, message = "El stock no puede ser negativo")
        @Schema(description = "Unidades disponibles; null = sin control de stock")
        Integer stock
) {
}
