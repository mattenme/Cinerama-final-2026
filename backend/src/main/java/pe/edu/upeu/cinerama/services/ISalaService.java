package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.SalaDTO;

import java.util.List;
import java.util.Optional;

public interface ISalaService {
    List<SalaDTO> listarTodo();
    List<SalaDTO> listarActivas();
    Optional<SalaDTO> buscarPorId(Long id);
    SalaDTO crear(SalaDTO dto);
    Optional<SalaDTO> actualizar(Long id, SalaDTO dto);
    SalaDTO cambiarActivo(Long id);
    boolean eliminar(Long id);
}
