package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.PromocionDTO;

import java.util.List;
import java.util.Optional;

public interface IPromocionService {
    List<PromocionDTO> listarTodo();
    List<PromocionDTO> listarActivas();
    Optional<PromocionDTO> buscarPorId(Long id);
    Optional<PromocionDTO> buscarPorCodigo(String codigo);
    PromocionDTO crear(PromocionDTO dto);
    Optional<PromocionDTO> actualizar(Long id, PromocionDTO dto);
    PromocionDTO cambiarActivo(Long id);
    boolean eliminar(Long id);
}
