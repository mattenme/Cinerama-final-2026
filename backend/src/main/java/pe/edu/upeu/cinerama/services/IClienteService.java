package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.ClienteDTO;

import java.util.List;
import java.util.Optional;

public interface IClienteService {
    List<ClienteDTO> listarTodo();
    Optional<ClienteDTO> buscarPorId(Long id);
    boolean existePorEmail(String email);
    boolean existePorDni(String dni);
    ClienteDTO cambiarActivo(Long id);
    boolean eliminar(Long id);
}
