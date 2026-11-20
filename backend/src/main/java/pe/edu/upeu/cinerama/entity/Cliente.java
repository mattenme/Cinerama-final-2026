package pe.edu.upeu.cinerama.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "CLIENTE")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CLIENTE")
    private Long idCliente;

    @Column(name = "DNI", unique = true, length = 20)
    private String dni;

    @Column(name = "NOMBRE", nullable = false, length = 150)
    private String nombre;

    @Column(name = "EMAIL", unique = true, length = 150)
    private String email;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "PASSWORD", length = 255)
    private String contrasena;

    @Column(name = "ROL", length = 20)
    private String rol = "CLIENTE";

    @Column(name = "ACTIVO", nullable = false)
    private Integer activo = 1;

    @Column(name = "VERIFICADO", nullable = false)
    private Integer verificado = 0;

    @Column(name = "CODIGO_VERIFICACION", length = 100)
    private String codigoVerificacion;

    @Column(name = "CODIGO_VERIFICACION_EXPIRA")
    private Timestamp codigoVerificacionExpira;

    @Column(name = "INTENTOS_FALLIDOS")
    private Integer intentosFallidos = 0;

    @Column(name = "BLOQUEADO_HASTA")
    private Timestamp bloqueadoHasta;

    public Cliente() {
    }

    public Cliente(Long idCliente, String dni, String nombre, String email, String telefono,
                   String contrasena, String rol, Integer activo, Integer verificado,
                   String codigoVerificacion) {
        this.idCliente = idCliente;
        this.dni = dni;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.contrasena = contrasena;
        this.rol = rol;
        this.activo = activo;
        this.verificado = verificado;
        this.codigoVerificacion = codigoVerificacion;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Integer getActivo() {
        return activo;
    }

    public void setActivo(Integer activo) {
        this.activo = activo;
    }

    public Integer getVerificado() {
        return verificado;
    }

    public void setVerificado(Integer verificado) {
        this.verificado = verificado;
    }

    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public void setCodigoVerificacion(String codigoVerificacion) {
        this.codigoVerificacion = codigoVerificacion;
    }

    public Timestamp getCodigoVerificacionExpira() {
        return codigoVerificacionExpira;
    }

    public void setCodigoVerificacionExpira(Timestamp codigoVerificacionExpira) {
        this.codigoVerificacionExpira = codigoVerificacionExpira;
    }

    public Integer getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(Integer intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public Timestamp getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(Timestamp bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }
}
