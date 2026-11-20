package pe.edu.upeu.cinerama.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.edu.upeu.cinerama.dto.CompraDTO;
import pe.edu.upeu.cinerama.dto.CompraRequestDTO;
import pe.edu.upeu.cinerama.dto.ItemDulceriaDTO;
import pe.edu.upeu.cinerama.entity.Asiento;
import pe.edu.upeu.cinerama.entity.Cliente;
import pe.edu.upeu.cinerama.entity.Compra;
import pe.edu.upeu.cinerama.entity.CompraDetalle;
import pe.edu.upeu.cinerama.entity.Funcion;
import pe.edu.upeu.cinerama.entity.Producto;
import pe.edu.upeu.cinerama.entity.Promocion;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.AsientoRepository;
import pe.edu.upeu.cinerama.repository.ClienteRepository;
import pe.edu.upeu.cinerama.repository.CompraDetalleRepository;
import pe.edu.upeu.cinerama.repository.CompraRepository;
import pe.edu.upeu.cinerama.repository.FuncionRepository;
import pe.edu.upeu.cinerama.repository.ProductoRepository;
import pe.edu.upeu.cinerama.repository.PromocionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CompraServiceImpl implements ICompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraServiceImpl.class);

    private static final int MAX_LONGITUD_PRODUCTOS = 2000;
    private static final Set<String> METODOS_PAGO = Set.of("TARJETA", "EFECTIVO", "YAPE", "PLIN");
    private static final int MAX_LONGITUD_METODO_PAGO = 50;
    private static final List<String> ESTADOS_OCUPADO =
            List.of("COMPLETADA", "CONFIRMADA", "PENDIENTE");

    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;
    private final ClienteRepository clienteRepository;
    private final FuncionRepository funcionRepository;
    private final AsientoRepository asientoRepository;
    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;
    private final IEmailService emailService;
    private final IQRService qrService;

    public CompraServiceImpl(CompraRepository compraRepository,
                             CompraDetalleRepository compraDetalleRepository,
                             ClienteRepository clienteRepository,
                             FuncionRepository funcionRepository,
                             AsientoRepository asientoRepository,
                             ProductoRepository productoRepository,
                             PromocionRepository promocionRepository,
                             IEmailService emailService,
                             IQRService qrService) {
        this.compraRepository = compraRepository;
        this.compraDetalleRepository = compraDetalleRepository;
        this.clienteRepository = clienteRepository;
        this.funcionRepository = funcionRepository;
        this.asientoRepository = asientoRepository;
        this.productoRepository = productoRepository;
        this.promocionRepository = promocionRepository;
        this.emailService = emailService;
        this.qrService = qrService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraDTO> listarTodo() {
        return compraRepository.findAllConRelaciones().stream()
                .flatMap(compra -> convertir(compra).stream())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraDTO> listarPorCliente(Long idCliente) {
        return compraRepository.findByClienteConRelaciones(idCliente).stream()
                .flatMap(compra -> convertir(compra).stream())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraDTO> listarPorFuncion(Long idFuncion) {
        return compraRepository.findByFuncionConRelaciones(idFuncion).stream()
                .flatMap(compra -> convertir(compra).stream())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompraDTO> listarConfirmadasPorFuncion(Long idFuncion) {
        return compraRepository.findConfirmedByFuncion(idFuncion).stream()
                .flatMap(compra -> convertir(compra).stream())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listarAsientosOcupados(Long idFuncion) {
        return compraDetalleRepository.findAsientosOcupados(idFuncion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompraDTO> buscarPorId(Long id) {
        return compraRepository.findByIdConRelaciones(id)
                .flatMap(compra -> convertir(compra).stream().findFirst());
    }

    @Override
    @Transactional
    public List<CompraDTO> procesarCompra(CompraRequestDTO dto, Long idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente));

        Funcion funcion = funcionRepository.findByIdWithRelations(dto.funcionId())
                .orElseThrow(() -> new ResourceNotFoundException("Función no encontrada con id: " + dto.funcionId()));

        if (funcion.getActivo() != null && funcion.getActivo() == 0) {
            throw new BadRequestException("La función ya no está disponible");
        }

        List<Long> idsSolicitados = dto.asientosIds() == null ? List.of() : dto.asientosIds();
        if (idsSolicitados.isEmpty()) {
            throw new BadRequestException("Debe seleccionar al menos un asiento");
        }

        LinkedHashSet<Long> idsUnicos = new LinkedHashSet<>(idsSolicitados);
        if (idsUnicos.size() != idsSolicitados.size()) {
            throw new BadRequestException("No se puede repetir el mismo asiento en la compra");
        }

        List<Asiento> asientos = asientoRepository.findAllById(idsUnicos);
        if (asientos.size() != idsUnicos.size()) {
            throw new BadRequestException("Uno o más asientos no existen");
        }

        for (Asiento asiento : asientos) {
            if (!asiento.getSala().getIdSala().equals(funcion.getSala().getIdSala())) {
                throw new BadRequestException(
                        "El asiento " + asiento.getFila() + asiento.getNumero() + " no pertenece a la sala de la función");
            }
            if (isAsientoOcupado(funcion.getIdFuncion(), asiento.getIdAsiento())) {
                throw new BadRequestException(
                        "El asiento " + asiento.getFila() + asiento.getNumero() + " ya está ocupado para esta función");
            }
        }

        BigDecimal precioUnitario = calcularPrecioUnitario(funcion, dto.codigoPromocion());

        BigDecimal totalDulceria = BigDecimal.ZERO;
        String textoProductos = null;
        List<CompraServiceImpl.ProductoPedido> pedidos = new ArrayList<>();

        if (dto.productos() != null && !dto.productos().isEmpty()) {
            Map<Long, Integer> cantidades = new LinkedHashMap<>();
            for (ItemDulceriaDTO item : dto.productos()) {
                int cantidad = item.cantidad() == null ? 0 : item.cantidad();
                if (cantidad < 1) {
                    throw new BadRequestException("La cantidad de un producto de dulcería no es válida");
                }
                cantidades.merge(item.idProducto(), cantidad, Integer::sum);
            }

            StringBuilder texto = new StringBuilder();
            for (Map.Entry<Long, Integer> entrada : cantidades.entrySet()) {
                Producto producto = productoRepository.findById(entrada.getKey())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + entrada.getKey()));
                if (producto.getActivo() != null && producto.getActivo() == 0) {
                    throw new BadRequestException("El producto " + producto.getNombre() + " ya no está disponible");
                }

                int cantidad = entrada.getValue();
                Integer stock = producto.getStock();
                if (stock != null && stock < cantidad) {
                    throw new BadRequestException(stock <= 0
                            ? "El producto " + producto.getNombre() + " está agotado"
                            : "Solo quedan " + stock + " unidades de " + producto.getNombre());
                }

                BigDecimal precio = producto.getPrecio() == null ? BigDecimal.ZERO : producto.getPrecio();
                totalDulceria = totalDulceria.add(precio.multiply(BigDecimal.valueOf(cantidad)));
                pedidos.add(new CompraServiceImpl.ProductoPedido(producto, cantidad));

                if (!texto.isEmpty()) {
                    texto.append(", ");
                }
                texto.append(producto.getNombre()).append(" x").append(cantidad);
            }
            textoProductos = texto.toString();
            if (textoProductos.length() > MAX_LONGITUD_PRODUCTOS) {
                textoProductos = textoProductos.substring(0, MAX_LONGITUD_PRODUCTOS);
            }
        }

        String codigoQr = "CIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Timestamp fechaCompra = Timestamp.valueOf(LocalDateTime.now());
        String metodoPago = normalizarMetodoPago(dto.metodoPago(), dto.referenciaPago());

        BigDecimal totalProductos = totalDulceria.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEntradas = precioUnitario.multiply(BigDecimal.valueOf(asientos.size()));

        String asientosStr = asientos.stream()
                .map(a -> a.getFila() + a.getNumero())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setFuncion(funcion);
        compra.setPrecio(totalEntradas);
        compra.setTotalProductos(totalProductos);
        compra.setMetodoPago(metodoPago);
        compra.setEstado(Compra.ESTADO_COMPLETADA);
        compra.setCodigoQr(codigoQr);
        compra.setFechaCompra(fechaCompra);
        compra.setProductos(textoProductos);
        compra.setAsientos(asientosStr);

        for (Asiento asiento : asientos) {
            CompraDetalle detalle = new CompraDetalle();
            detalle.setCompra(compra);
            detalle.setFuncion(funcion);
            detalle.setAsiento(asiento);
            detalle.setPrecio(precioUnitario);
            compra.getDetalles().add(detalle);
        }

        for (CompraServiceImpl.ProductoPedido pedido : pedidos) {
            if (pedido.producto().getStock() == null) {
                continue;
            }
            int descontadas = productoRepository.descontarStock(
                    pedido.producto().getIdProducto(), pedido.cantidad());
            if (descontadas == 0) {
                throw new BadRequestException(
                        "El producto " + pedido.producto().getNombre() + " se acaba de agotar. Ajusta tu pedido.");
            }
        }

        Compra guardada;
        try {
            guardada = compraRepository.save(compra);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException(
                    "Alguna de las butacas se acaba de ocupar. Vuelve a elegir tus asientos.");
        }

        programarCorreoConfirmacion(
                cliente.getEmail(),
                cliente.getNombre(),
                funcion.getPelicula().getTitulo(),
                funcion.getSala().getNombre(),
                asientosStr,
                totalEntradas.add(totalProductos),
                codigoQr,
                fechaCompra);

        return convertir(guardada);
    }

    @Override
    @Transactional
    public Optional<CompraDTO> cancelar(Long id, boolean esAdmin) {
        Compra compra = compraRepository.findByIdConRelaciones(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));

        if (Compra.ESTADO_CANCELADA.equalsIgnoreCase(compra.getEstado())) {
            throw new BadRequestException("La compra ya está cancelada");
        }
        if (Integer.valueOf(1).equals(compra.getUsado())) {
            throw new BadRequestException("La entrada ya fue utilizada; no se puede cancelar");
        }
        if (!esAdmin && compra.getFuncion().getHoraInicio() != null
                && compra.getFuncion().getHoraInicio().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("La función ya comenzó; ya no se puede cancelar");
        }

        String asientos = compra.getAsientos();
        if (asientos == null || asientos.isBlank()) {
            asientos = compra.getDetalles().stream()
                    .map(d -> d.getAsiento().getFila() + d.getAsiento().getNumero())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(null);
        }

        compra.setEstado(Compra.ESTADO_CANCELADA);
        compra.setFechaCancelacion(Timestamp.valueOf(LocalDateTime.now()));
        compra.setAsientos(asientos);

        reponerDulceria(compra.getProductos());

        compra.getDetalles().clear();

        Compra guardada = compraRepository.save(compra);
        return convertir(guardada).stream().findFirst();
    }

    private void reponerDulceria(String productos) {
        if (productos == null || productos.isBlank()) {
            return;
        }

        for (String linea : productos.split(",")) {
            String texto = linea.trim();
            int marca = texto.lastIndexOf(" x");
            if (marca <= 0) {
                continue;
            }
            String nombre = texto.substring(0, marca).trim();
            int cantidad;
            try {
                cantidad = Integer.parseInt(texto.substring(marca + 2).trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (cantidad <= 0) {
                continue;
            }

            productoRepository.findByNombre(nombre).forEach(producto -> {
                if (producto.getStock() != null) {
                    productoRepository.reponerStock(producto.getIdProducto(), cantidad);
                }
            });
        }
    }

    private record ProductoPedido(Producto producto, int cantidad) {
    }

    private boolean isAsientoOcupado(Long idFuncion, Long idAsiento) {
        return compraDetalleRepository.existsByFuncion_IdFuncionAndAsiento_IdAsientoAndCompra_EstadoIn(
                idFuncion, idAsiento, ESTADOS_OCUPADO);
    }

    private BigDecimal calcularPrecioUnitario(Funcion funcion, String codigoPromocion) {
        BigDecimal base = funcion.getPrecioEfectivo();

        if (codigoPromocion == null || codigoPromocion.isBlank()) {
            return base;
        }

        Promocion promocion = promocionRepository
                .findByCodigoIgnoreCase(codigoPromocion.trim())
                .orElseThrow(() -> new BadRequestException("El código de promoción no existe"));

        if (promocion.getActivo() != null && promocion.getActivo() == 0) {
            throw new BadRequestException("La promoción ya no está vigente");
        }

        int descuento = promocion.getDescuento() == null ? 0 : promocion.getDescuento();
        descuento = Math.max(0, Math.min(100, descuento));

        BigDecimal factor = BigDecimal.valueOf(100 - descuento);
        return base.multiply(factor)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void programarCorreoConfirmacion(String email, String nombre, String pelicula, String sala,
                                             String asientos, BigDecimal total, String codigoQr,
                                             Timestamp fechaCompra) {
        Runnable envio = () -> enviarCorreo(
                email, nombre, pelicula, sala, asientos, total, codigoQr, fechaCompra);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    private void enviarCorreo(String email, String nombre, String pelicula, String sala,
                              String asientosStr, BigDecimal total, String codigoQr, Timestamp fechaCompra) {
        try {
            String fechaStr = fechaCompra.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            emailService.enviarConfirmacionCompra(
                    email,
                    nombre,
                    pelicula,
                    sala,
                    fechaStr,
                    asientosStr,
                    total.doubleValue(),
                    codigoQr,
                    qrService.generarBytes(codigoQr)
            );
        } catch (Exception e) {
            log.warn("Error enviando correo de confirmación: {}", e.getMessage());
        }
    }

    private String normalizarMetodoPago(String metodo, String referencia) {
        String limpio = metodo == null ? "" : metodo.trim().toUpperCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            throw new BadRequestException("Debe indicar un método de pago");
        }
        if (!METODOS_PAGO.contains(limpio)) {
            throw new BadRequestException("Método de pago no válido: " + limpio);
        }

        String etiqueta = limpio;
        if (referencia != null && !referencia.isBlank()) {
            String detalle = referencia.replaceAll("[^A-Za-z0-9* -]", "").trim();
            if (!detalle.isEmpty()) {
                etiqueta = limpio + " " + detalle;
            }
        }

        return etiqueta.length() > MAX_LONGITUD_METODO_PAGO
                ? etiqueta.substring(0, MAX_LONGITUD_METODO_PAGO)
                : etiqueta;
    }

    private List<CompraDTO> convertir(Compra compra) {
        List<CompraDetalle> detalles = new ArrayList<>(compra.getDetalles());
        detalles.sort(Comparator
                .comparing((CompraDetalle d) -> d.getAsiento().getFila())
                .thenComparing(d -> d.getAsiento().getNumero()));

        if (detalles.isEmpty()) {
            return List.of(convertirLinea(compra, null, compra.getPrecio(), compra.getTotalProductos()));
        }

        List<CompraDTO> filas = new ArrayList<>(detalles.size());
        boolean primera = true;
        for (CompraDetalle detalle : detalles) {
            filas.add(convertirLinea(compra, detalle, detalle.getPrecio(),
                    primera ? compra.getTotalProductos() : null));
            primera = false;
        }
        return filas;
    }

    private CompraDTO convertirLinea(Compra compra, CompraDetalle detalle, BigDecimal precio,
                                     BigDecimal totalProductos) {
        Funcion funcion = compra.getFuncion();
        Asiento asiento = detalle == null ? null : detalle.getAsiento();

        return new CompraDTO(
                compra.getIdCompra(),
                compra.getCliente().getIdCliente(),
                compra.getCliente().getNombre(),
                funcion.getIdFuncion(),
                funcion.getPelicula().getTitulo(),
                funcion.getSala().getNombre(),
                funcion.getHoraInicio(),
                funcion.getPelicula().getIdPelicula(),
                funcion.getHoraFin(),
                asiento != null ? asiento.getIdAsiento() : null,
                asiento != null ? asiento.getFila() : compra.getAsientos(),
                asiento != null ? asiento.getNumero() : null,
                precio,
                totalProductos,
                compra.getMetodoPago(),
                compra.getEstado(),
                compra.getCodigoQr(),
                compra.getFechaCompra() != null ? compra.getFechaCompra().toLocalDateTime() : null,
                compra.getProductos(),
                compra.getUsado(),
                compra.getFechaUso() != null ? compra.getFechaUso().toLocalDateTime() : null,
                compra.getFechaCancelacion() != null ? compra.getFechaCancelacion().toLocalDateTime() : null
        );
    }
}
