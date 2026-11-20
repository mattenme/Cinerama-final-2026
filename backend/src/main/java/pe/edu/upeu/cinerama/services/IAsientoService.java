package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.AsientoDTO;

import java.util.List;
import java.util.Optional;

public interface IAsientoService {
    List<AsientoDTO> listarTodo();
    List<AsientoDTO> listarPorSala(Long idSala);
    List<AsientoDTO> listarDisponiblesPorSala(Long idSala);
    long contarDisponiblesPorSala(Long idSala);
    Optional<AsientoDTO> buscarPorId(Long id);
    AsientoDTO crear(AsientoDTO dto);
    List<AsientoDTO> generarButacas(Long idSala, int filas, int butacasPorFila);
    boolean eliminar(Long id);
}
