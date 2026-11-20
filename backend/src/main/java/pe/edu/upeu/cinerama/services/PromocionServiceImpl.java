package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.PromocionDTO;
import pe.edu.upeu.cinerama.entity.Promocion;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.PromocionRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PromocionServiceImpl implements IPromocionService {

    private final PromocionRepository promocionRepository;

    public PromocionServiceImpl(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromocionDTO> listarTodo() {
        return promocionRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromocionDTO> listarActivas() {
        return promocionRepository.findByActivo(1).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PromocionDTO> buscarPorId(Long id) {
        return promocionRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PromocionDTO> buscarPorCodigo(String codigo) {
        return promocionRepository.findByCodigoIgnoreCase(codigo.trim()).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public PromocionDTO crear(PromocionDTO dto) {
        Promocion promocion = new Promocion();
        promocion.setCodigo(dto.codigo().trim().toUpperCase());
        promocion.setDescripcion(dto.descripcion());
        promocion.setDescuento(dto.descuento());
        promocion.setActivo(dto.activo() == null ? 1 : dto.activo());
        return convertToDTO(promocionRepository.save(promocion));
    }

    @Override
    @Transactional
    public Optional<PromocionDTO> actualizar(Long id, PromocionDTO dto) {
        return promocionRepository.findById(id).map(promocion -> {
            promocion.setCodigo(dto.codigo().trim().toUpperCase());
            promocion.setDescripcion(dto.descripcion());
            promocion.setDescuento(dto.descuento());
            if (dto.activo() != null) {
                promocion.setActivo(dto.activo());
            }
            return convertToDTO(promocionRepository.save(promocion));
        });
    }

    @Override
    @Transactional
    public PromocionDTO cambiarActivo(Long id) {
        Promocion promocion = promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + id));
        promocion.setActivo(promocion.getActivo() == 1 ? 0 : 1);
        return convertToDTO(promocionRepository.save(promocion));
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return promocionRepository.findById(id).map(promocion -> {
            promocionRepository.delete(promocion);
            return true;
        }).orElse(false);
    }

    private PromocionDTO convertToDTO(Promocion promocion) {
        return new PromocionDTO(
                promocion.getIdPromocion(),
                promocion.getCodigo(),
                promocion.getDescripcion(),
                promocion.getDescuento(),
                promocion.getActivo()
        );
    }
}
