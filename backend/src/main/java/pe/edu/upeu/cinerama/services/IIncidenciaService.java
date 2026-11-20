package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.IncidenciaDTO;
import pe.edu.upeu.cinerama.dto.IncidenciaRequestDTO;

import java.util.List;
import java.util.Optional;

public interface IIncidenciaService {
    List<IncidenciaDTO> listarTodo();
    List<IncidenciaDTO> listarPendientes();
    List<IncidenciaDTO> listarPorCliente(Long idCliente);
    List<IncidenciaDTO> listarPorEstado(String estado);
    Optional<IncidenciaDTO> buscarPorId(Long id);
    IncidenciaDTO crear(IncidenciaRequestDTO dto, Long idCliente);
    Optional<IncidenciaDTO> actualizar(Long id, IncidenciaRequestDTO dto);
    IncidenciaDTO cambiarEstado(Long id, String estado);
    boolean eliminar(Long id);
}
