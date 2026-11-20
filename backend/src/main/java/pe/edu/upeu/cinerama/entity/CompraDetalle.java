package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "COMPRA_DETALLE",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_COMPRA_DETALLE_FUNCION_ASIENTO",
                columnNames = {"ID_FUNCION", "ID_ASIENTO"}))
public class CompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DETALLE")
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_COMPRA", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_FUNCION", nullable = false)
    private Funcion funcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_ASIENTO", nullable = false)
    private Asiento asiento;

    @Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    public CompraDetalle() {
    }

    public CompraDetalle(Long idDetalle, Compra compra, Funcion funcion, Asiento asiento, BigDecimal precio) {
        this.idDetalle = idDetalle;
        this.compra = compra;
        this.funcion = funcion;
        this.asiento = asiento;
        this.precio = precio;
    }

    public Long getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Long idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Funcion getFuncion() {
        return funcion;
    }

    public void setFuncion(Funcion funcion) {
        this.funcion = funcion;
    }

    public Asiento getAsiento() {
        return asiento;
    }

    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
