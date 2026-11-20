package pe.edu.upeu.cinerama.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.cinerama.dto.CompraDTO;
import pe.edu.upeu.cinerama.dto.CompraRequestDTO;
import pe.edu.upeu.cinerama.dto.ItemDulceriaDTO;
import pe.edu.upeu.cinerama.entity.Asiento;
import pe.edu.upeu.cinerama.entity.Cliente;
import pe.edu.upeu.cinerama.entity.Compra;
import pe.edu.upeu.cinerama.entity.Funcion;
import pe.edu.upeu.cinerama.entity.Pelicula;
import pe.edu.upeu.cinerama.entity.Producto;
import pe.edu.upeu.cinerama.entity.Sala;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.repository.AsientoRepository;
import pe.edu.upeu.cinerama.repository.ClienteRepository;
import pe.edu.upeu.cinerama.repository.CompraDetalleRepository;
import pe.edu.upeu.cinerama.repository.CompraRepository;
import pe.edu.upeu.cinerama.repository.FuncionRepository;
import pe.edu.upeu.cinerama.repository.ProductoRepository;
import pe.edu.upeu.cinerama.repository.PromocionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {

    private static final BigDecimal PRECIO_BUTACA = new BigDecimal("20.00");

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private CompraDetalleRepository compraDetalleRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private FuncionRepository funcionRepository;
    @Mock
    private AsientoRepository asientoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PromocionRepository promocionRepository;
    @Mock
    private IEmailService emailService;
    @Mock
    private IQRService qrService;

    @InjectMocks
    private CompraServiceImpl service;

    private Cliente cliente;
    private Funcion funcion;
    private Sala sala;
    private Asiento asiento;

    @BeforeEach
    void setUp() {
        sala = new Sala();
        sala.setIdSala(1L);
        sala.setNombre("Sala 1");

        Pelicula pelicula = new Pelicula();
        pelicula.setIdPelicula(1L);
        pelicula.setTitulo("Dune");
        pelicula.setDuracionMinutos(120);

        funcion = new Funcion();
        funcion.setIdFuncion(5L);
        funcion.setSala(sala);
        funcion.setPelicula(pelicula);
        funcion.setPrecio(PRECIO_BUTACA);
        funcion.setActivo(1);
        funcion.setHoraInicio(LocalDateTime.now().plusHours(3));

        cliente = new Cliente();
        cliente.setIdCliente(7L);
        cliente.setNombre("Ana");
        cliente.setEmail("ana@correo.com");

        asiento = new Asiento();
        asiento.setIdAsiento(11L);
        asiento.setSala(sala);
        asiento.setFila("A");
        asiento.setNumero(1);

        // lenient: no todas las pruebas llegan a resolver cliente y función.
        lenient().when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        lenient().when(funcionRepository.findByIdWithRelations(5L)).thenReturn(Optional.of(funcion));
    }

    private CompraRequestDTO pedido(List<Long> asientos, List<ItemDulceriaDTO> productos) {
        return new CompraRequestDTO(5L, asientos, "TARJETA", "**** 1111", productos, null);
    }

    @Test
    void rechazaButacaRepetidaEnLaMismaCompra() {
        assertThatThrownBy(() -> service.procesarCompra(pedido(List.of(11L, 11L), null), 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("repetir");

        verify(compraRepository, never()).save(any());
    }

    @Test
    void rechazaButacaYaVendida() {
        given(asientoRepository.findAllById(any())).willReturn(List.of(asiento));
        given(compraDetalleRepository.existsByFuncion_IdFuncionAndAsiento_IdAsientoAndCompra_EstadoIn(
                anyLong(), anyLong(), any())).willReturn(true);

        assertThatThrownBy(() -> service.procesarCompra(pedido(List.of(11L), null), 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ocupado");

        verify(compraRepository, never()).save(any());
    }

    @Test
    void rechazaDulceriaSinStockSuficiente() {
        given(asientoRepository.findAllById(any())).willReturn(List.of(asiento));
        given(compraDetalleRepository.existsByFuncion_IdFuncionAndAsiento_IdAsientoAndCompra_EstadoIn(
                anyLong(), anyLong(), any())).willReturn(false);

        Producto producto = new Producto();
        producto.setIdProducto(3L);
        producto.setNombre("Combo");
        producto.setPrecio(new BigDecimal("15.00"));
        producto.setActivo(1);
        producto.setStock(1);
        given(productoRepository.findById(3L)).willReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.procesarCompra(
                pedido(List.of(11L), List.of(new ItemDulceriaDTO(3L, 2))), 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Solo quedan 1");

        verify(compraRepository, never()).save(any());
    }

    @Test
    void abortaSiElStockSeAgotoEntreLaValidacionYElDescuento() {
        given(asientoRepository.findAllById(any())).willReturn(List.of(asiento));
        given(compraDetalleRepository.existsByFuncion_IdFuncionAndAsiento_IdAsientoAndCompra_EstadoIn(
                anyLong(), anyLong(), any())).willReturn(false);

        Producto producto = new Producto();
        producto.setIdProducto(3L);
        producto.setNombre("Combo");
        producto.setPrecio(new BigDecimal("15.00"));
        producto.setActivo(1);
        producto.setStock(5);
        given(productoRepository.findById(3L)).willReturn(Optional.of(producto));
        given(productoRepository.descontarStock(3L, 2)).willReturn(0);

        assertThatThrownBy(() -> service.procesarCompra(
                pedido(List.of(11L), List.of(new ItemDulceriaDTO(3L, 2))), 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("agot");

        verify(compraRepository, never()).save(any());
    }

    @Test
    void compraValidaDescuentaStockYGuardaLosTotales() {
        given(asientoRepository.findAllById(any())).willReturn(List.of(asiento));
        given(compraDetalleRepository.existsByFuncion_IdFuncionAndAsiento_IdAsientoAndCompra_EstadoIn(
                anyLong(), anyLong(), any())).willReturn(false);

        Producto producto = new Producto();
        producto.setIdProducto(3L);
        producto.setNombre("Combo");
        producto.setPrecio(new BigDecimal("15.00"));
        producto.setActivo(1);
        producto.setStock(5);
        given(productoRepository.findById(3L)).willReturn(Optional.of(producto));
        given(productoRepository.descontarStock(3L, 2)).willReturn(1);
        given(qrService.generarBytes(anyString())).willReturn(new byte[]{1, 2, 3});
        given(compraRepository.save(any(Compra.class))).willAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setIdCompra(99L);
            return c;
        });

        List<CompraDTO> filas = service.procesarCompra(
                pedido(List.of(11L), List.of(new ItemDulceriaDTO(3L, 2))), 7L);

        assertThat(filas).hasSize(1);
        CompraDTO fila = filas.get(0);
        assertThat(fila.estado()).isEqualTo(Compra.ESTADO_COMPLETADA);
        assertThat(fila.precio()).isEqualByComparingTo(PRECIO_BUTACA);
        assertThat(fila.totalProductos()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(fila.codigoQr()).startsWith("CIN-");
        assertThat(fila.productos()).contains("Combo x2");

        verify(productoRepository).descontarStock(3L, 2);
        verify(compraRepository).save(any(Compra.class));
    }

    @Test
    void cancelarLiberaLaCompraYReponeElStock() {
        Compra compra = new Compra();
        compra.setIdCompra(50L);
        compra.setCliente(cliente);
        compra.setFuncion(funcion);
        compra.setPrecio(new BigDecimal("40.00"));
        compra.setTotalProductos(new BigDecimal("30.00"));
        compra.setProductos("Combo x2");
        compra.setEstado(Compra.ESTADO_COMPLETADA);
        compra.setUsado(0);
        given(compraRepository.findByIdConRelaciones(50L)).willReturn(Optional.of(compra));
        given(compraRepository.save(any(Compra.class))).willAnswer(inv -> inv.getArgument(0));

        Producto combo = new Producto();
        combo.setIdProducto(3L);
        combo.setNombre("Combo");
        combo.setStock(1);
        given(productoRepository.findByNombre("Combo")).willReturn(List.of(combo));

        service.cancelar(50L, false).orElseThrow();

        assertThat(compra.getEstado()).isEqualTo(Compra.ESTADO_CANCELADA);
        assertThat(compra.getFechaCancelacion()).isNotNull();
        assertThat(compra.getDetalles()).isEmpty();
        verify(productoRepository).reponerStock(3L, 2);
    }

    @Test
    void noDescuentaStockDeProductosSinControl() {
        given(asientoRepository.findAllById(any())).willReturn(List.of(asiento));
        given(compraDetalleRepository.existsByFuncion_IdFuncionAndAsiento_IdAsientoAndCompra_EstadoIn(
                anyLong(), anyLong(), any())).willReturn(false);

        Producto producto = new Producto();
        producto.setIdProducto(4L);
        producto.setNombre("Agua");
        producto.setPrecio(new BigDecimal("4.00"));
        producto.setActivo(1);
        producto.setStock(null);
        given(productoRepository.findById(4L)).willReturn(Optional.of(producto));
        given(compraRepository.save(any(Compra.class))).willAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setIdCompra(100L);
            return c;
        });

        service.procesarCompra(pedido(List.of(11L), List.of(new ItemDulceriaDTO(4L, 1))), 7L);

        verify(productoRepository, never()).descontarStock(anyLong(), anyInt());
    }
}
