package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COMPRA")
public class Compra {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_COMPLETADA = "COMPLETADA";
    public static final String ESTADO_CANCELADA = "CANCELADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_COMPRA")
    private Long idCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLIENTE", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_FUNCION", nullable = false)
    private Funcion funcion;

    @Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "METODO_PAGO", length = 50)
    private String metodoPago;

    @Column(name = "ESTADO", length = 50)
    private String estado = ESTADO_PENDIENTE;

    @Column(name = "CODIGO_QR", length = 500)
    private String codigoQr;

    @Column(name = "FECHA_COMPRA", nullable = false)
    private Timestamp fechaCompra;

    @Column(name = "PRODUCTOS", length = 2000)
    private String productos;

    @Column(name = "ASIENTOS", length = 500)
    private String asientos;

    @Column(name = "TOTAL_PRODUCTOS", precision = 10, scale = 2)
    private BigDecimal totalProductos;

    @Column(name = "USADO")
    private Integer usado = 0;

    @Column(name = "FECHA_USO")
    private Timestamp fechaUso;

    @Column(name = "FECHA_CANCELACION")
    private Timestamp fechaCancelacion;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<CompraDetalle> detalles = new ArrayList<>();

    public Compra() {
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaCompra == null) {
            this.fechaCompra = new Timestamp(System.currentTimeMillis());
        }
        if (this.estado == null || this.estado.isBlank()) {
            this.estado = ESTADO_PENDIENTE;
        }
        if (this.usado == null) {
            this.usado = 0;
        }
    }

    public Long getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Long idCompra) {
        this.idCompra = idCompra;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Funcion getFuncion() {
        return funcion;
    }

    public void setFuncion(Funcion funcion) {
        this.funcion = funcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public Timestamp getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(Timestamp fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public String getAsientos() {
        return asientos;
    }

    public void setAsientos(String asientos) {
        this.asientos = asientos;
    }

    public String getProductos() {
        return productos;
    }

    public void setProductos(String productos) {
        this.productos = productos;
    }

    public BigDecimal getTotalProductos() {
        return totalProductos;
    }

    public void setTotalProductos(BigDecimal totalProductos) {
        this.totalProductos = totalProductos;
    }

    public List<CompraDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<CompraDetalle> detalles) {
        this.detalles = detalles;
    }

    public Integer getUsado() {
        return usado;
    }

    public void setUsado(Integer usado) {
        this.usado = usado;
    }

    public Timestamp getFechaUso() {
        return fechaUso;
    }

    public void setFechaUso(Timestamp fechaUso) {
        this.fechaUso = fechaUso;
    }

    public Timestamp getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(Timestamp fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

}
