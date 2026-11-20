package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "PELICULA")
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PELICULA")
    private Long idPelicula;

    @Column(name = "TITULO", nullable = false, length = 200)
    private String titulo;

    @Column(name = "DURACION_MINUTOS", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "GENERO", length = 100)
    private String genero;

    @Column(name = "SINOPSIS", length = 2000)
    private String sinopsis;

    @Column(name = "IMAGEN_URL", length = 500)
    private String imagenUrl;

    @Column(name = "DESTACADO", nullable = false)
    private Integer destacado = 0;

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo = 1;

    public Pelicula() {
    }

    public Pelicula(Long idPelicula, String titulo, Integer duracionMinutos, String genero,
                    String sinopsis, String imagenUrl, Integer destacado, Integer activo) {
        this.idPelicula = idPelicula;
        this.titulo = titulo;
        this.duracionMinutos = duracionMinutos;
        this.genero = genero;
        this.sinopsis = sinopsis;
        this.imagenUrl = imagenUrl;
        this.destacado = destacado;
        this.activo = activo;
    }

    public Long getIdPelicula() {
        return idPelicula;
    }

    public void setIdPelicula(Long idPelicula) {
        this.idPelicula = idPelicula;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public Integer getDestacado() {
        return destacado;
    }

    public void setDestacado(Integer destacado) {
        this.destacado = destacado;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }
}
