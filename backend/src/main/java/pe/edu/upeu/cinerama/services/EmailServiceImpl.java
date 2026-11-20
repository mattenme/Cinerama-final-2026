package pe.edu.upeu.cinerama.services;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.upeu.cinerama.dto.PruebaCorreoDTO;

import java.time.LocalDateTime;
import java.util.Properties;

@Service
public class EmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${cinerama.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${cinerama.mail.port:587}")
    private String port;

    @Value("${cinerama.mail.username:}")
    private String username;

    @Value("${cinerama.mail.password:}")
    private String password;

    @Value("${cinerama.mail.from:}")
    private String from;

    @Value("${cinerama.mail.debug:false}")
    private boolean debug;

    private final BandejaCorreoService bandeja;

    public EmailServiceImpl(BandejaCorreoService bandeja) {
        this.bandeja = bandeja;
    }

    private Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", debug);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    @Override
    public boolean enviarCorreo(String to, String subject, String html) {
        try {
            String sender = (from == null || from.isBlank()) ? username : from;
            if (sender == null || sender.isBlank()) {
                log.warn("No se envió correo: remitente no configurado.");
                bandeja.registrar(to, subject, false,
                        "SMTP sin configurar: el correo solo se guardó en esta bandeja", null, html);
                return false;
            }

            Message msg = new MimeMessage(getSession());
            msg.setFrom(new InternetAddress(sender));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setContent(html, "text/html; charset=utf-8");
            Transport.send(msg);
            bandeja.registrar(to, subject, true, "Enviado por " + host + ":" + port, null, html);
            return true;
        } catch (Exception e) {
            log.error("Error enviando correo a {}: {}", to, e.getMessage());
            bandeja.registrar(to, subject, false, "El SMTP falló: " + e.getMessage(), null, html);
            return false;
        }
    }

    @Override
    public PruebaCorreoDTO probarEnvio(String destino) {
        LocalDateTime ahora = LocalDateTime.now();
        String remitente = (from == null || from.isBlank()) ? username : from;
        String servidor = host + ":" + port;

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return new PruebaCorreoDTO(false, destino, servidor, remitente,
                    "Faltan MAIL_USERNAME y MAIL_PASSWORD en la configuración", ahora);
        }
        if (remitente == null || remitente.isBlank()) {
            return new PruebaCorreoDTO(false, destino, servidor, null,
                    "Falta configurar el remitente (MAIL_FROM o MAIL_USERNAME)", ahora);
        }

        try {
            String html = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#ffc107;padding:20px;text-align:center;'><h2 style='margin:0;color:#1a1a2e;'>Cinerama</h2></div>"
                + "<div style='padding:30px;text-align:center;'>"
                + "<h3 style='color:#333;margin:0 0 10px;'>Prueba de correo</h3>"
                + "<p style='color:#666;font-size:14px;'>Si recibes este mensaje, el SMTP de Cinerama está configurado correctamente.</p>"
                + "<p style='color:#999;font-size:12px;'>Enviado el " + ahora + " desde " + servidor + "</p>"
                + "</div></div></body></html>";

            Message msg = new MimeMessage(getSession());
            msg.setFrom(new InternetAddress(remitente));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino));
            msg.setSubject("Cinerama - Prueba de configuración SMTP");
            msg.setContent(html, "text/html; charset=utf-8");
            Transport.send(msg);

            return new PruebaCorreoDTO(true, destino, servidor, remitente,
                    "Correo de prueba enviado; revisa la bandeja de destino", ahora);
        } catch (Exception e) {
            String detalle = (e.getMessage() == null || e.getMessage().isBlank())
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
            return new PruebaCorreoDTO(false, destino, servidor, remitente,
                    "El SMTP rechazó el envío: " + detalle, ahora);
        }
    }

    @Override
    public boolean enviarCodigoVerificacion(String email, String codigo) {
        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;'>"
            + "<div style='max-width:480px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
            + "<div style='background:#ffc107;padding:20px;text-align:center;'><h2 style='margin:0;color:#1a1a2e;'>Cinerama</h2></div>"
            + "<div style='padding:30px;text-align:center;'>"
            + "<h3 style='color:#333;margin:0 0 10px;'>Verifica tu cuenta</h3>"
            + "<p style='color:#666;font-size:14px;'>Usa este código para completar tu registro:</p>"
            + "<div style='background:#f8f9fc;border-radius:8px;padding:16px;margin:20px 0;letter-spacing:8px;font-size:28px;font-weight:bold;color:#1a1a2e;'>" + codigo + "</div>"
            + "<p style='color:#999;font-size:12px;'>El código expira en 10 minutos.</p>"
            + "</div></div></body></html>";
        return enviarCorreo(email, "Cinerama - Código de verificación", html);
    }

    @Override
    public boolean enviarConfirmacionCompra(String email, String nombre, String pelicula, String sala,
                                            String fecha, String asientos, double total,
                                            String qrCode, byte[] qrBytes) {
        try {
            String html = "<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:520px;margin:40px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#ffc107;padding:20px;text-align:center;'><h2 style='margin:0;color:#1a1a2e;'>Cinerama</h2></div>"
                + "<div style='padding:30px;'>"
                + "<h3 style='color:#333;margin:0 0 5px;'>¡Compra confirmada!</h3>"
                + "<p style='color:#666;font-size:14px;margin:0 0 20px;'>Hola <strong>" + nombre + "</strong>, aquí tienes los detalles:</p>"
                + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                + "<tr><td style='padding:8px 0;color:#888;'>Película</td><td style='padding:8px 0;font-weight:600;text-align:right;'>" + pelicula + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#888;border-top:1px solid #eee;'>Sala</td><td style='padding:8px 0;font-weight:600;text-align:right;border-top:1px solid #eee;'>" + sala + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#888;border-top:1px solid #eee;'>Fecha / Hora</td><td style='padding:8px 0;font-weight:600;text-align:right;border-top:1px solid #eee;'>" + fecha + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#888;border-top:1px solid #eee;'>Asientos</td><td style='padding:8px 0;font-weight:600;text-align:right;border-top:1px solid #eee;'>" + asientos + "</td></tr>"
                + "<tr><td style='padding:8px 0;color:#888;border-top:1px solid #eee;'>Total</td><td style='padding:8px 0;font-weight:700;text-align:right;border-top:1px solid #eee;color:#1a1a2e;font-size:16px;'>S/ " + String.format("%.2f", total) + "</td></tr>"
                + "</table>"
                + "<div style='text-align:center;margin:24px 0 0;'>"
                + "<p style='color:#888;font-size:13px;margin:0 0 8px;'>Presenta este código en taquilla</p>"
                + "<img src='cid:qr002' alt='QR' style='width:160px;height:160px;border:2px solid #eee;border-radius:8px;padding:8px;'>"
                + "<div style='font-size:14px;font-weight:bold;letter-spacing:3px;color:#1a1a2e;margin-top:8px;'>" + qrCode + "</div>"
                + "</div></div></div></body></html>";

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html, "text/html; charset=utf-8");

            MimeBodyPart imagePart = new MimeBodyPart();
            imagePart.setDataHandler(new DataHandler(new ByteArrayDataSource(qrBytes, "image/png")));
            imagePart.setHeader("Content-ID", "<qr002>");
            imagePart.setDisposition(MimeBodyPart.INLINE);

            Multipart mp = new MimeMultipart();
            mp.addBodyPart(htmlPart);
            mp.addBodyPart(imagePart);

            String asunto = "Cinerama - Confirmación de compra";
            String sender = (from == null || from.isBlank()) ? username : from;
            if (sender == null || sender.isBlank()) {
                log.warn("No se envió correo de compra: remitente no configurado.");
                bandeja.registrar(email, asunto, false,
                        "SMTP sin configurar: la entrada quedó guardada en esta bandeja", qrCode, html);
                return false;
            }

            Message msg = new MimeMessage(getSession());
            msg.setFrom(new InternetAddress(sender));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            msg.setSubject(asunto);
            msg.setContent(mp);
            Transport.send(msg);
            bandeja.registrar(email, asunto, true, "Enviado por " + host + ":" + port, qrCode, html);
            return true;
        } catch (Exception e) {
            log.error("Error enviando correo a {}: {}", email, e.getMessage());
            return false;
        }
    }
}
