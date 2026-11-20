package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.CalificacionDTO;
import pe.edu.upeu.cinerama.dto.CalificacionRequestDTO;
import pe.edu.upeu.cinerama.dto.PuedeCalificarDTO;
import pe.edu.upeu.cinerama.entity.Calificacion;
import pe.edu.upeu.cinerama.entity.CalificacionId;
import pe.edu.upeu.cinerama.entity.Compra;
import pe.edu.upeu.cinerama.entity.Funcion;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.repository.CalificacionRepository;
import pe.edu.upeu.cinerama.repository.CompraRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class CalificacionServiceImpl implements ICalificacionService {

    private final CalificacionRepository calificacionRepository;
    private final CompraRepository compraRepository;

    public CalificacionServiceImpl(CalificacionRepository calificacionRepository,
                                   CompraRepository compraRepository) {
        this.calificacionRepository = calificacionRepository;
        this.compraRepository = compraRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalificacionDTO> listarTodo() {
        return calificacionRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalificacionDTO> listarPorPelicula(Long idPelicula) {
        return calificacionRepository.findByIdPelicula(idPelicula).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalificacionDTO> listarPorCliente(Long idCliente) {
        return calificacionRepository.findByIdCliente(idCliente).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalificacionDTO> buscarPorClienteYPelicula(Long idCliente, Long idPelicula) {
        return calificacionRepository.findByIdClienteAndIdPelicula(idCliente, idPelicula)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Double promedioPorPelicula(Long idPelicula) {
        return calificacionRepository.findAverageByPelicula(idPelicula);
    }

    @Override
    @Transactional(readOnly = true)
    public Long contarPorPelicula(Long idPelicula) {
        return calificacionRepository.countByPelicula(idPelicula);
    }

    @Override
    @Transactional
    public CalificacionDTO registrar(Long idCliente, CalificacionRequestDTO dto) {
        PuedeCalificarDTO elegibilidad = elegibilidad(idCliente, dto.idPelicula());
        if (!elegibilidad.puede()) {
            throw new BadRequestException(elegibilidad.mensaje());
        }

        Calificacion calificacion = calificacionRepository
                .findByIdClienteAndIdPelicula(idCliente, dto.idPelicula())
                .orElseGet(() -> {
                    Calificacion nueva = new Calificacion();
                    nueva.setIdCliente(idCliente);
                    nueva.setIdPelicula(dto.idPelicula());
                    nueva.setFechaCalificacion(new Timestamp(System.currentTimeMillis()));
                    return nueva;
                });

        calificacion.setPuntuacion(dto.puntuacion());
        calificacion.setComentario(dto.comentario());
        return convertToDTO(calificacionRepository.save(calificacion));
    }

    @Override
    @Transactional
    public boolean eliminar(Long idCliente, Long idPelicula) {
        CalificacionId id = new CalificacionId(idCliente, idPelicula);
        return calificacionRepository.findById(id).map(calificacion -> {
            calificacionRepository.delete(calificacion);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public PuedeCalificarDTO elegibilidad(Long idCliente, Long idPelicula) {
        if (idCliente == null) {
            return new PuedeCalificarDTO(false, "Inicia sesión para calificar esta película");
        }

        List<Compra> compras = compraRepository.findByClienteYPelicula(idCliente, idPelicula);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime proximoFin = null;
        boolean tieneCompra = false;

        for (Compra compra : compras) {
            if (Compra.ESTADO_CANCELADA.equalsIgnoreCase(compra.getEstado())) {
                continue;
            }
            Funcion funcion = compra.getFuncion();
            if (funcion == null || funcion.getHoraInicio() == null) {
                continue;
            }

            tieneCompra = true;
            LocalDateTime fin = funcion.getHoraFin();

            if (!fin.isAfter(ahora)) {
                return new PuedeCalificarDTO(true, "Ya puedes calificar esta película");
            }
            if (proximoFin == null || fin.isBefore(proximoFin)) {
                proximoFin = fin;
            }
        }

        if (!tieneCompra) {
            return new PuedeCalificarDTO(false,
                    "Solo puedes calificar después de comprar una entrada para esta película");
        }
        if (proximoFin == null) {
            return new PuedeCalificarDTO(false, "Podrás calificar después de ver la película");
        }
        return new PuedeCalificarDTO(false,
                "Podrás calificar cuando termine la función del "
                        + proximoFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }

    private CalificacionDTO convertToDTO(Calificacion calificacion) {
        return new CalificacionDTO(
                calificacion.getIdCliente(),
                calificacion.getIdPelicula(),
                calificacion.getPuntuacion(),
                calificacion.getComentario(),
                calificacion.getFechaCalificacion() != null
                        ? calificacion.getFechaCalificacion().toLocalDateTime()
                        : null,
                calificacion.getCliente() != null ? calificacion.getCliente().getNombre() : null
        );
    }
}
