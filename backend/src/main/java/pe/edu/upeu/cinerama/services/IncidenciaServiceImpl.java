package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.IncidenciaDTO;
import pe.edu.upeu.cinerama.dto.IncidenciaRequestDTO;
import pe.edu.upeu.cinerama.entity.Incidencia;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.ClienteRepository;
import pe.edu.upeu.cinerama.repository.FuncionRepository;
import pe.edu.upeu.cinerama.repository.IncidenciaRepository;
import pe.edu.upeu.cinerama.repository.SalaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class IncidenciaServiceImpl implements IIncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final SalaRepository salaRepository;
    private final FuncionRepository funcionRepository;
    private final ClienteRepository clienteRepository;

    public IncidenciaServiceImpl(IncidenciaRepository incidenciaRepository,
                                 SalaRepository salaRepository,
                                 FuncionRepository funcionRepository,
                                 ClienteRepository clienteRepository) {
        this.incidenciaRepository = incidenciaRepository;
        this.salaRepository = salaRepository;
        this.funcionRepository = funcionRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaDTO> listarTodo() {
        return incidenciaRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaDTO> listarPendientes() {
        return incidenciaRepository.findAllPending().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaDTO> listarPorCliente(Long idCliente) {
        return incidenciaRepository.findByCliente_IdClienteOrderByFechaReporteDesc(idCliente).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaDTO> listarPorEstado(String estado) {
        return incidenciaRepository.findByEstado(estado).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncidenciaDTO> buscarPorId(Long id) {
        return incidenciaRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public IncidenciaDTO crear(IncidenciaRequestDTO dto, Long idCliente) {
        Incidencia incidencia = new Incidencia();
        aplicarDatos(incidencia, dto);
        incidencia.setCliente(idCliente == null
                ? null
                : clienteRepository.findById(idCliente).orElse(null));
        incidencia.setEstado("PENDIENTE");
        return convertToDTO(incidenciaRepository.save(incidencia));
    }

    @Override
    @Transactional
    public Optional<IncidenciaDTO> actualizar(Long id, IncidenciaRequestDTO dto) {
        return incidenciaRepository.findById(id).map(incidencia -> {
            aplicarDatos(incidencia, dto);
            if (dto.estado() != null && !dto.estado().isBlank()) {
                incidencia.setEstado(dto.estado());
            }
            return convertToDTO(incidenciaRepository.save(incidencia));
        });
    }

    @Override
    @Transactional
    public IncidenciaDTO cambiarEstado(Long id, String estado) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada con id: " + id));
        incidencia.setEstado(estado);
        return convertToDTO(incidenciaRepository.save(incidencia));
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return incidenciaRepository.findById(id).map(incidencia -> {
            incidenciaRepository.delete(incidencia);
            return true;
        }).orElse(false);
    }

    private void aplicarDatos(Incidencia incidencia, IncidenciaRequestDTO dto) {
        incidencia.setTipo(dto.tipo());
        incidencia.setDescripcion(dto.descripcion());
        incidencia.setSala(dto.idSala() == null ? null : salaRepository.findById(dto.idSala()).orElse(null));
        incidencia.setFuncion(dto.idFuncion() == null ? null : funcionRepository.findById(dto.idFuncion()).orElse(null));
    }

    private IncidenciaDTO convertToDTO(Incidencia incidencia) {
        return new IncidenciaDTO(
                incidencia.getIdIncidencia(),
                incidencia.getTipo(),
                incidencia.getDescripcion(),
                incidencia.getFechaReporte() != null
                        ? incidencia.getFechaReporte().toLocalDateTime()
                        : null,
                incidencia.getEstado(),
                incidencia.getSala() != null ? incidencia.getSala().getIdSala() : null,
                incidencia.getFuncion() != null ? incidencia.getFuncion().getIdFuncion() : null,
                incidencia.getCliente() != null ? incidencia.getCliente().getIdCliente() : null,
                incidencia.getCliente() != null ? incidencia.getCliente().getNombre() : null,
                incidencia.getSala() != null ? incidencia.getSala().getNombre() : null,
                incidencia.getFuncion() != null && incidencia.getFuncion().getPelicula() != null
                        ? incidencia.getFuncion().getPelicula().getTitulo()
                        : null
        );
    }
}
