package pe.edu.upeu.cinerama.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.SalaDTO;
import pe.edu.upeu.cinerama.entity.Sala;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.AsientoRepository;
import pe.edu.upeu.cinerama.repository.SalaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class SalaServiceImpl implements ISalaService {

    private final SalaRepository salaRepository;
    private final AsientoRepository asientoRepository;

    public SalaServiceImpl(SalaRepository salaRepository, AsientoRepository asientoRepository) {
        this.salaRepository = salaRepository;
        this.asientoRepository = asientoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaDTO> listarTodo() {
        return salaRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaDTO> listarActivas() {
        return salaRepository.findByActivoOrderByNombreAsc(1).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalaDTO> buscarPorId(Long id) {
        return salaRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public SalaDTO crear(SalaDTO dto) {
        Sala sala = new Sala();
        sala.setNombre(dto.nombre());
        sala.setTipo(dto.tipo());
        sala.setCapacidadTotal(dto.capacidadTotal() == null ? 0 : dto.capacidadTotal());
        sala.setActivo(dto.activo() == null ? 1 : dto.activo());
        return convertToDTO(salaRepository.save(sala));
    }

    @Override
    @Transactional
    public Optional<SalaDTO> actualizar(Long id, SalaDTO dto) {
        return salaRepository.findById(id).map(sala -> {
            sala.setNombre(dto.nombre());
            sala.setTipo(dto.tipo());
            if (dto.capacidadTotal() != null) {
                sala.setCapacidadTotal(dto.capacidadTotal());
            }
            if (dto.activo() != null) {
                sala.setActivo(dto.activo());
            }
            return convertToDTO(salaRepository.save(sala));
        });
    }

    @Override
    @Transactional
    public SalaDTO cambiarActivo(Long id) {
        Sala sala = salaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada con id: " + id));
        sala.setActivo(sala.getActivo() == 1 ? 0 : 1);
        return convertToDTO(salaRepository.save(sala));
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return salaRepository.findById(id).map(sala -> {
            if (asientoRepository.countBySala_IdSala(id) > 0) {
                throw new BadRequestException(
                        "La sala tiene butacas registradas: desactívala o elimina sus butacas primero");
            }
            try {
                salaRepository.delete(sala);
                salaRepository.flush();
            } catch (DataIntegrityViolationException e) {
                throw new BadRequestException(
                        "La sala tiene funciones u otros registros asociados: desactívala en lugar de eliminarla");
            }
            return true;
        }).orElse(false);
    }

    private SalaDTO convertToDTO(Sala sala) {
        return new SalaDTO(
                sala.getIdSala(),
                sala.getNombre(),
                sala.getTipo(),
                sala.getCapacidadTotal(),
                sala.getActivo()
        );
    }
}
