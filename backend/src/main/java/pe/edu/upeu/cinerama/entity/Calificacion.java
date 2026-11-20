package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "CALIFICACION")
@IdClass(CalificacionId.class)
public class Calificacion {

    @Id
    @Column(name = "ID_CLIENTE", nullable = false)
    private Long idCliente;

    @Id
    @Column(name = "ID_PELICULA", nullable = false)
    private Long idPelicula;

    @Column(name = "PUNTUACION", nullable = false)
    private Integer puntuacion;

    @Column(name = "COMENTARIO", length = 2000)
    private String comentario;

    @Column(name = "FECHA_CALIFICACION", nullable = false)
    private Timestamp fechaCalificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CLIENTE", insertable = false, updatable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_PELICULA", insertable = false, updatable = false)
    private Pelicula pelicula;

    public Calificacion() {
    }

    public Calificacion(Long idCliente, Long idPelicula, Integer puntuacion, String comentario,
                        Timestamp fechaCalificacion, Cliente cliente, Pelicula pelicula) {
        this.idCliente = idCliente;
        this.idPelicula = idPelicula;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.fechaCalificacion = fechaCalificacion;
        this.cliente = cliente;
        this.pelicula = pelicula;
    }

    @PrePersist
    public void prePersist() {
        if (this.fechaCalificacion == null) {
            this.fechaCalificacion = new Timestamp(System.currentTimeMillis());
        }
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Long getIdPelicula() {
        return idPelicula;
    }

    public void setIdPelicula(Long idPelicula) {
        this.idPelicula = idPelicula;
    }

    public Integer getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Integer puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Timestamp getFechaCalificacion() {
        return fechaCalificacion;
    }

    public void setFechaCalificacion(Timestamp fechaCalificacion) {
        this.fechaCalificacion = fechaCalificacion;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Pelicula getPelicula() {
        return pelicula;
    }

    public void setPelicula(Pelicula pelicula) {
        this.pelicula = pelicula;
    }
}
