package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.FuncionDTO;
import pe.edu.upeu.cinerama.dto.FuncionRequestDTO;
import pe.edu.upeu.cinerama.entity.Funcion;
import pe.edu.upeu.cinerama.entity.Pelicula;
import pe.edu.upeu.cinerama.entity.Sala;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.FuncionRepository;
import pe.edu.upeu.cinerama.repository.PeliculaRepository;
import pe.edu.upeu.cinerama.repository.SalaRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class FuncionServiceImpl implements IFuncionService {

    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final SalaRepository salaRepository;

    public FuncionServiceImpl(FuncionRepository funcionRepository,
                              PeliculaRepository peliculaRepository,
                              SalaRepository salaRepository) {
        this.funcionRepository = funcionRepository;
        this.peliculaRepository = peliculaRepository;
        this.salaRepository = salaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuncionDTO> listarTodo() {
        return funcionRepository.findAllWithRelations().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuncionDTO> listarActivas() {
        return funcionRepository.findAllActive().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuncionDTO> listarPorPelicula(Long idPelicula) {
        return funcionRepository.findByPeliculaIdActive(idPelicula).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FuncionDTO> listarPorSala(Long idSala) {
        return funcionRepository.findBySalaIdActiveWithRelations(idSala).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuncionDTO> buscarPorId(Long id) {
        return funcionRepository.findByIdWithRelations(id).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public FuncionDTO crear(FuncionRequestDTO dto) {
        Funcion funcion = aplicarDatos(new Funcion(), dto);
        Funcion guardada = funcionRepository.save(funcion);
        return convertToDTO(funcionRepository.findByIdWithRelations(guardada.getIdFuncion()).orElse(guardada));
    }

    @Override
    @Transactional
    public Optional<FuncionDTO> actualizar(Long id, FuncionRequestDTO dto) {
        return funcionRepository.findByIdWithRelations(id).map(funcion -> {
            aplicarDatos(funcion, dto);
            funcionRepository.save(funcion);
            return funcionRepository.findByIdWithRelations(id)
                    .map(this::convertToDTO)
                    .orElseGet(() -> convertToDTO(funcion));
        });
    }

    @Override
    @Transactional
    public FuncionDTO cambiarActivo(Long id) {
        Funcion funcion = funcionRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con id: " + id));
        funcion.setActivo(funcion.getActivo() == 1 ? 0 : 1);
        return convertToDTO(funcionRepository.save(funcion));
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return funcionRepository.findById(id).map(funcion -> {
            funcionRepository.delete(funcion);
            return true;
        }).orElse(false);
    }

    private Funcion aplicarDatos(Funcion funcion, FuncionRequestDTO dto) {
        Pelicula pelicula = peliculaRepository.findById(dto.idPelicula())
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con id: " + dto.idPelicula()));

        Sala sala = salaRepository.findById(dto.idSala())
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada con id: " + dto.idSala()));

        if (dto.horaInicio() == null) {
            throw new BadRequestException("La fecha y hora son obligatorias");
        }

        funcion.setPelicula(pelicula);
        funcion.setSala(sala);
        funcion.setHoraInicio(dto.horaInicio());

        String estado = dto.estado();
        funcion.setEstado(estado == null || estado.isBlank()
                ? "PROGRAMADA"
                : estado.trim().toUpperCase(Locale.ROOT));

        funcion.setActivo(dto.activo() == null ? 1 : dto.activo());
        if (dto.precio() != null) {
            funcion.setPrecio(dto.precio());
        } else if (funcion.getPrecio() == null) {
            funcion.setPrecio(funcion.getPrecioEfectivo());
        }

        validarChoqueDeHorario(funcion);
        return funcion;
    }

    private void validarChoqueDeHorario(Funcion funcion) {
        LocalDateTime inicio = funcion.getHoraInicio();
        LocalDateTime fin = inicio.plusMinutes(duracionDe(funcion.getPelicula()));

        List<Funcion> cercanas = funcionRepository.findActivasEnRango(
                funcion.getSala().getIdSala(), inicio.minusHours(6), inicio.plusHours(6));

        for (Funcion otra : cercanas) {
            if (otra.getIdFuncion() != null && otra.getIdFuncion().equals(funcion.getIdFuncion())) {
                continue;
            }
            LocalDateTime otraInicio = otra.getHoraInicio();
            LocalDateTime otraFin = otraInicio.plusMinutes(duracionDe(otra.getPelicula()));

            if (inicio.isBefore(otraFin) && fin.isAfter(otraInicio)) {
                DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
                throw new BadRequestException(String.format(
                        "La sala %s ya tiene \"%s\" de %s a %s; elige otro horario",
                        funcion.getSala().getNombre(),
                        otra.getPelicula().getTitulo(),
                        otraInicio.format(hora), otraFin.format(hora)));
            }
        }
    }

    private int duracionDe(Pelicula pelicula) {
        Integer duracion = pelicula != null ? pelicula.getDuracionMinutos() : null;
        return duracion != null && duracion > 0 ? duracion : 120;
    }

    private FuncionDTO convertToDTO(Funcion funcion) {
        return new FuncionDTO(
                funcion.getIdFuncion(),
                funcion.getPelicula().getIdPelicula(),
                funcion.getPelicula().getTitulo(),
                funcion.getSala().getIdSala(),
                funcion.getSala().getNombre(),
                funcion.getHoraInicio(),
                funcion.getEstado(),
                funcion.getActivo(),
                funcion.getPrecioEfectivo()
        );
    }
}
