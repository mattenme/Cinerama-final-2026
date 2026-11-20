package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.CalificacionDTO;
import pe.edu.upeu.cinerama.dto.CalificacionRequestDTO;
import pe.edu.upeu.cinerama.dto.PuedeCalificarDTO;

import java.util.List;
import java.util.Optional;

public interface ICalificacionService {
    List<CalificacionDTO> listarTodo();
    List<CalificacionDTO> listarPorPelicula(Long idPelicula);
    List<CalificacionDTO> listarPorCliente(Long idCliente);
    Optional<CalificacionDTO> buscarPorClienteYPelicula(Long idCliente, Long idPelicula);
    Double promedioPorPelicula(Long idPelicula);
    Long contarPorPelicula(Long idPelicula);
    PuedeCalificarDTO elegibilidad(Long idCliente, Long idPelicula);
    CalificacionDTO registrar(Long idCliente, CalificacionRequestDTO dto);
    boolean eliminar(Long idCliente, Long idPelicula);
}
