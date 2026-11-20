package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.ValidacionDTO;
import pe.edu.upeu.cinerama.entity.Compra;
import pe.edu.upeu.cinerama.entity.CompraDetalle;
import pe.edu.upeu.cinerama.entity.Validacion;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.repository.CompraRepository;
import pe.edu.upeu.cinerama.repository.ValidacionRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PuertaServiceImpl implements IPuertaService {

    private static final String COMPLETADA = "COMPLETADA";
    private static final String CONFIRMADA = "CONFIRMADA";
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CompraRepository compraRepository;
    private final ValidacionRepository validacionRepository;

    public PuertaServiceImpl(CompraRepository compraRepository,
                             ValidacionRepository validacionRepository) {
        this.compraRepository = compraRepository;
        this.validacionRepository = validacionRepository;
    }

    @Override
    @Transactional
    public ValidacionDTO validar(String codigo, Long idEmpleado) {
        String limpio = normalizar(codigo);
        if (limpio.isEmpty()) {
            throw new BadRequestException("Escanea o escribe el código QR");
        }

        List<Compra> encontradas = compraRepository.buscarPorCodigoQrParaValidar(limpio);
        if (encontradas.isEmpty()) {
            return registrar(null, limpio, idEmpleado, Validacion.RESULTADO_NO_EXISTE,
                    "El código no corresponde a ninguna compra");
        }

        Compra compra = encontradas.get(0);

        if (Compra.ESTADO_CANCELADA.equalsIgnoreCase(compra.getEstado())) {
            return registrar(compra, limpio, idEmpleado, Validacion.RESULTADO_CANCELADA,
                    "La compra fue cancelada y reembolsada");
        }

        if (!COMPLETADA.equalsIgnoreCase(compra.getEstado())
                && !CONFIRMADA.equalsIgnoreCase(compra.getEstado())) {
            return registrar(compra, limpio, idEmpleado, Validacion.RESULTADO_NO_VALIDA,
                    "La compra no está pagada");
        }

        if (Integer.valueOf(1).equals(compra.getUsado())) {
            String fecha = compra.getFechaUso() != null
                    ? " el " + compra.getFechaUso().toLocalDateTime().format(FECHA_HORA)
                    : "";
            return registrar(compra, limpio, idEmpleado, Validacion.RESULTADO_YA_USADO,
                    "La entrada ya fue validada" + fecha);
        }

        compra.setUsado(1);
        compra.setFechaUso(Timestamp.valueOf(LocalDateTime.now()));
        compraRepository.save(compra);

        return registrar(compra, limpio, idEmpleado, Validacion.RESULTADO_VALIDO,
                "Entrada válida. ¡Buen cine!");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidacionDTO> listarRecientes() {
        return validacionRepository.findRecientes().stream()
                .limit(50)
                .map(this::convertir)
                .toList();
    }

    private ValidacionDTO registrar(Compra compra, String codigo, Long idEmpleado,
                                    String resultado, String mensaje) {
        Validacion validacion = new Validacion();
        validacion.setCompra(compra);
        validacion.setCodigoQr(codigo);
        validacion.setIdEmpleado(idEmpleado);
        validacion.setResultado(resultado);
        validacion.setMensaje(mensaje);
        validacion.setFecha(Timestamp.valueOf(LocalDateTime.now()));

        return convertir(validacionRepository.save(validacion));
    }

    private ValidacionDTO convertir(Validacion validacion) {
        Compra compra = validacion.getCompra();

        String asientos = null;
        String pelicula = null;
        String sala = null;
        LocalDateTime horaInicio = null;
        String cliente = null;

        if (compra != null) {
            asientos = compra.getDetalles().isEmpty()
                    ? compra.getAsientos()
                    : compra.getDetalles().stream()
                            .map(this::etiqueta)
                            .sorted()
                            .collect(Collectors.joining(", "));
            pelicula = compra.getFuncion().getPelicula().getTitulo();
            sala = compra.getFuncion().getSala().getNombre();
            horaInicio = compra.getFuncion().getHoraInicio();
            cliente = compra.getCliente().getNombre();
        }

        return new ValidacionDTO(
                validacion.getIdValidacion(),
                compra != null ? compra.getIdCompra() : null,
                validacion.getCodigoQr(),
                validacion.getResultado(),
                validacion.getMensaje(),
                validacion.getFecha() != null ? validacion.getFecha().toLocalDateTime() : null,
                validacion.getIdEmpleado(),
                pelicula,
                sala,
                horaInicio,
                asientos,
                cliente,
                compra != null ? compra.getUsado() : null,
                compra != null && compra.getFechaUso() != null
                        ? compra.getFechaUso().toLocalDateTime()
                        : null
        );
    }

    private String etiqueta(CompraDetalle detalle) {
        return detalle.getAsiento().getFila() + detalle.getAsiento().getNumero();
    }

    private String normalizar(String codigo) {
        if (codigo == null) {
            return "";
        }
        String valor = codigo.trim().toUpperCase(Locale.ROOT);
        int barra = valor.lastIndexOf('/');
        if (barra >= 0) {
            valor = valor.substring(barra + 1);
        }
        return valor;
    }
}
