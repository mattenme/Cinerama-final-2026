package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "SALA")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SALA")
    private Long idSala;

    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;

    @Column(name = "TIPO", length = 50)
    private String tipo;

    @Column(name = "CAPACIDAD_TOTAL", nullable = false)
    private Integer capacidadTotal = 0;

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo = 1;

    public Sala() {
    }

    public Sala(Long idSala, String nombre, String tipo, Integer capacidadTotal, Integer activo) {
        this.idSala = idSala;
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidadTotal = capacidadTotal;
        this.activo = activo;
    }

    public Long getIdSala() {
        return idSala;
    }

    public void setIdSala(Long idSala) {
        this.idSala = idSala;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidadTotal() {
        return capacidadTotal;
    }

    public void setCapacidadTotal(Integer capacidadTotal) {
        this.capacidadTotal = capacidadTotal;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }
}
