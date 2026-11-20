package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CalificacionId implements Serializable {

    private Long idCliente;
    private Long idPelicula;

    public CalificacionId() {
    }

    public CalificacionId(Long idCliente, Long idPelicula) {
        this.idCliente = idCliente;
        this.idPelicula = idPelicula;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalificacionId that)) return false;
        return Objects.equals(idCliente, that.idCliente)
                && Objects.equals(idPelicula, that.idPelicula);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente, idPelicula);
    }
}
