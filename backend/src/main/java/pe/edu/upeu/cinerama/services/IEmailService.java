package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.PruebaCorreoDTO;

public interface IEmailService {
    boolean enviarCorreo(String to, String subject, String html);
    PruebaCorreoDTO probarEnvio(String destino);
    boolean enviarCodigoVerificacion(String email, String codigo);
    boolean enviarConfirmacionCompra(String email, String nombre, String pelicula, String sala,
                                     String fecha, String asientos, double total,
                                     String qrCode, byte[] qrBytes);
}
