package pe.edu.upeu.cinerama.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromocionDTO(
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long idPromocion,

        @NotBlank(message = "El código es obligatorio")
        String codigo,

        String descripcion,

        @NotNull(message = "El descuento es obligatorio")
        @Min(value = 0, message = "El descuento no puede ser negativo")
        @Max(value = 100, message = "El descuento no puede superar 100")
        Integer descuento,

        Integer activo
) {
}
