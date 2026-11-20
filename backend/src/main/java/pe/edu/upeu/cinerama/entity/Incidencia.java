package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "INCIDENCIA")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_INCIDENCIA")
    private Long idIncidencia;

    @Column(name = "TIPO", nullable = false, length = 100)
    private String tipo;

    @Column(name = "DESCRIPCION", length = 2000)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SALA")
    private Sala sala;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_FUNCION")
    private Funcion funcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLIENTE")
    private Cliente cliente;

    @Column(name = "FECHA_REPORTE", nullable = false)
    private Timestamp fechaReporte;

    @Column(name = "ESTADO", length = 50)
    private String estado = "PENDIENTE";

    public Incidencia() {
    }

    public Incidencia(Long idIncidencia, String tipo, String descripcion, Sala sala, Funcion funcion,
                      Cliente cliente, Timestamp fechaReporte, String estado) {
        this.idIncidencia = idIncidencia;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.sala = sala;
        this.funcion = funcion;
        this.cliente = cliente;
        this.fechaReporte = fechaReporte;
        this.estado = estado;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaReporte == null) {
            this.fechaReporte = new Timestamp(System.currentTimeMillis());
        }
        if (this.estado == null || this.estado.isBlank()) {
            this.estado = "PENDIENTE";
        }
    }

    public Long getIdIncidencia() {
        return idIncidencia;
    }

    public void setIdIncidencia(Long idIncidencia) {
        this.idIncidencia = idIncidencia;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public Funcion getFuncion() {
        return funcion;
    }

    public void setFuncion(Funcion funcion) {
        this.funcion = funcion;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Timestamp getFechaReporte() {
        return fechaReporte;
    }

    public void setFechaReporte(Timestamp fechaReporte) {
        this.fechaReporte = fechaReporte;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
