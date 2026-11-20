package pe.edu.upeu.cinerama.dto;

public record CalificacionPromedioDTO(
        Long idPelicula,
        Double promedio,
        Long total
) {
}
