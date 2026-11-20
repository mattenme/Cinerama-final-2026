package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ASIENTO")
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ASIENTO")
    private Long idAsiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SALA", nullable = false)
    private Sala sala;

    @Column(name = "FILA", nullable = false, length = 5)
    private String fila;

    @Column(name = "NUMERO", nullable = false)
    private Integer numero;

    @Column(name = "ESTADO", length = 20)
    private String estado = "Disponible";

    public Asiento() {
    }

    public Asiento(Long idAsiento, Sala sala, String fila, Integer numero, String estado) {
        this.idAsiento = idAsiento;
        this.sala = sala;
        this.fila = fila;
        this.numero = numero;
        this.estado = estado;
    }

    public Long getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Long idAsiento) {
        this.idAsiento = idAsiento;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
