package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "FUNCION")
public class Funcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_FUNCION")
    private Long idFuncion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PELICULA", nullable = false)
    private Pelicula pelicula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SALA", nullable = false)
    private Sala sala;

    @Column(name = "HORA_INICIO", nullable = false)
    private LocalDateTime horaInicio;

    @Column(name = "ESTADO", length = 50)
    private String estado = "PROGRAMADA";

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo = 1;

    @Column(name = "PRECIO", precision = 10, scale = 2)
    private BigDecimal precio;

    public Funcion() {
    }

    public Funcion(Long idFuncion, Pelicula pelicula, Sala sala, LocalDateTime horaInicio,
                   String estado, Integer activo) {
        this.idFuncion = idFuncion;
        this.pelicula = pelicula;
        this.sala = sala;
        this.horaInicio = horaInicio;
        this.estado = estado;
        this.activo = activo;
    }

    public Long getIdFuncion() {
        return idFuncion;
    }

    public void setIdFuncion(Long idFuncion) {
        this.idFuncion = idFuncion;
    }

    public Pelicula getPelicula() {
        return pelicula;
    }

    public void setPelicula(Pelicula pelicula) {
        this.pelicula = pelicula;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalDateTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalDateTime getHoraFin() {
        if (horaInicio == null) {
            return null;
        }
        Integer duracion = pelicula != null ? pelicula.getDuracionMinutos() : null;
        int minutos = duracion != null && duracion > 0 ? duracion : 120;
        return horaInicio.plusMinutes(minutos);
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getPrecioEfectivo() {
        if (precio != null) {
            return precio;
        }
        Integer duracion = pelicula != null ? pelicula.getDuracionMinutos() : null;
        return duracion != null && duracion > 120
                ? new BigDecimal("25.00")
                : new BigDecimal("20.00");
    }
}
