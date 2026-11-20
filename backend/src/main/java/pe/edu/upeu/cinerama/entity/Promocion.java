package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "PROMOCION")
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PROMOCION")
    private Long idPromocion;

    @Column(name = "CODIGO", unique = true, nullable = false, length = 50)
    private String codigo;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "DESCUENTO", nullable = false)
    private Integer descuento;

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo = 1;

    public Promocion() {
    }

    public Promocion(Long idPromocion, String codigo, String descripcion, Integer descuento, Integer activo) {
        this.idPromocion = idPromocion;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.descuento = descuento;
        this.activo = activo;
    }

    public Long getIdPromocion() {
        return idPromocion;
    }

    public void setIdPromocion(Long idPromocion) {
        this.idPromocion = idPromocion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDescuento() {
        return descuento;
    }

    public void setDescuento(Integer descuento) {
        this.descuento = descuento;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }
}
