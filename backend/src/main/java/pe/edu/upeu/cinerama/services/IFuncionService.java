package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.FuncionDTO;
import pe.edu.upeu.cinerama.dto.FuncionRequestDTO;

import java.util.List;
import java.util.Optional;

public interface IFuncionService {
    List<FuncionDTO> listarTodo();
    List<FuncionDTO> listarActivas();
    List<FuncionDTO> listarPorPelicula(Long idPelicula);
    List<FuncionDTO> listarPorSala(Long idSala);
    Optional<FuncionDTO> buscarPorId(Long id);
    FuncionDTO crear(FuncionRequestDTO dto);
    Optional<FuncionDTO> actualizar(Long id, FuncionRequestDTO dto);
    FuncionDTO cambiarActivo(Long id);
    boolean eliminar(Long id);
}
