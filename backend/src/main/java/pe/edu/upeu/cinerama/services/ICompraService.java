package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.CompraDTO;
import pe.edu.upeu.cinerama.dto.CompraRequestDTO;

import java.util.List;
import java.util.Optional;

public interface ICompraService {
    List<CompraDTO> listarTodo();
    List<CompraDTO> listarPorCliente(Long idCliente);
    List<CompraDTO> listarPorFuncion(Long idFuncion);
    List<CompraDTO> listarConfirmadasPorFuncion(Long idFuncion);
    List<Long> listarAsientosOcupados(Long idFuncion);
    Optional<CompraDTO> buscarPorId(Long id);
    List<CompraDTO> procesarCompra(CompraRequestDTO dto, Long idCliente);

    Optional<CompraDTO> cancelar(Long id, boolean esAdmin);
}
