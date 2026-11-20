package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "VALIDACION")
public class Validacion {

    public static final String RESULTADO_VALIDO = "VALIDO";
    public static final String RESULTADO_YA_USADO = "YA_USADO";
    public static final String RESULTADO_NO_EXISTE = "NO_EXISTE";
    public static final String RESULTADO_CANCELADA = "CANCELADA";
    public static final String RESULTADO_NO_VALIDA = "NO_VALIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_VALIDACION")
    private Long idValidacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_COMPRA")
    private Compra compra;

    @Column(name = "CODIGO_QR", nullable = false, length = 500)
    private String codigoQr;

    @Column(name = "ID_EMPLEADO")
    private Long idEmpleado;

    @Column(name = "FECHA", nullable = false)
    private Timestamp fecha;

    @Column(name = "RESULTADO", nullable = false, length = 30)
    private String resultado;

    @Column(name = "MENSAJE", length = 300)
    private String mensaje;

    public Validacion() {
    }

    @PrePersist
    public void prePersist() {
        if (this.fecha == null) {
            this.fecha = new Timestamp(System.currentTimeMillis());
        }
    }

    public Long getIdValidacion() {
        return idValidacion;
    }

    public void setIdValidacion(Long idValidacion) {
        this.idValidacion = idValidacion;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void setCodigoQr(String codigoQr) {
        this.codigoQr = codigoQr;
    }

    public Long getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(Long idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public Timestamp getFecha() {
        return fecha;
    }

    public void setFecha(Timestamp fecha) {
        this.fecha = fecha;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
