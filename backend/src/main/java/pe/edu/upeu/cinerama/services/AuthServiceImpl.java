package pe.edu.upeu.cinerama.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.AuthResponseDTO;
import pe.edu.upeu.cinerama.dto.LoginDTO;
import pe.edu.upeu.cinerama.dto.ReenvioCodigoDTO;
import pe.edu.upeu.cinerama.dto.RegistroDTO;
import pe.edu.upeu.cinerama.dto.VerificacionDTO;
import pe.edu.upeu.cinerama.entity.Cliente;
import pe.edu.upeu.cinerama.repository.ClienteRepository;
import pe.edu.upeu.cinerama.security.JwtService;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements IAuthService {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String HASH_SIN_USUARIO =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final JwtService jwtService;

    @Value("${cinerama.mail.expose-dev-code:false}")
    private boolean exponerCodigoDesarrollo;

    public AuthServiceImpl(ClienteRepository clienteRepository,
                           PasswordEncoder passwordEncoder,
                           IEmailService emailService,
                           JwtService jwtService) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginDTO dto) {
        Cliente cliente = clienteRepository.findByDni(dto.dni().trim()).orElse(null);

        if (cliente == null) {
            passwordEncoder.matches(dto.contrasena(), HASH_SIN_USUARIO);
            return respuesta(false, "DNI o contraseña incorrectos");
        }

        if (estaBloqueado(cliente)) {
            return respuesta(false, "Cuenta bloqueada temporalmente por intentos fallidos. Intenta de nuevo en "
                    + minutosRestantes(cliente) + " minuto(s).");
        }

        if (!passwordEncoder.matches(dto.contrasena(), cliente.getContrasena())) {
            registrarIntentoFallido(cliente);
            return respuesta(false, "DNI o contraseña incorrectos");
        }

        limpiarIntentos(cliente);

        if (Integer.valueOf(0).equals(cliente.getActivo())) {
            return respuesta(false, "La cuenta está desactivada");
        }
        if (Integer.valueOf(0).equals(cliente.getVerificado())) {
            return respuesta(false, "La cuenta no ha sido verificada. Revisa tu correo.");
        }
        String token = jwtService.generateToken(cliente);
        return new AuthResponseDTO(
                true,
                "Inicio de sesión exitoso",
                cliente.getRol(),
                cliente.getIdCliente(),
                cliente.getNombre(),
                null,
                token,
                jwtService.expiracionEnSegundos()
        );
    }

    private boolean estaBloqueado(Cliente cliente) {
        return cliente.getBloqueadoHasta() != null
                && cliente.getBloqueadoHasta().toLocalDateTime().isAfter(LocalDateTime.now());
    }

    private long minutosRestantes(Cliente cliente) {
        long segundos = Duration.between(LocalDateTime.now(),
                cliente.getBloqueadoHasta().toLocalDateTime()).getSeconds();
        return Math.max(1, (segundos + 59) / 60);
    }

    private void registrarIntentoFallido(Cliente cliente) {
        int intentos = (cliente.getIntentosFallidos() == null ? 0 : cliente.getIntentosFallidos()) + 1;
        cliente.setIntentosFallidos(intentos);

        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            cliente.setIntentosFallidos(0);
            cliente.setBloqueadoHasta(Timestamp.valueOf(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO)));
        }
        clienteRepository.save(cliente);
    }

    private static final int MINUTOS_VALIDEZ_CODIGO = 10;

    private void limpiarIntentos(Cliente cliente) {
        boolean habiaIntentos = cliente.getIntentosFallidos() != null && cliente.getIntentosFallidos() > 0;
        if (habiaIntentos || cliente.getBloqueadoHasta() != null) {
            cliente.setIntentosFallidos(0);
            cliente.setBloqueadoHasta(null);
            clienteRepository.save(cliente);
        }
    }

    @Override
    public AuthResponseDTO registrar(RegistroDTO dto) {
        if (clienteRepository.existsByDni(dto.dni().trim())) {
            return respuesta(false, "El DNI ya está registrado");
        }
        if (clienteRepository.existsByEmail(dto.email().trim())) {
            return respuesta(false, "El email ya está registrado");
        }

        Cliente cliente = new Cliente();
        cliente.setDni(dto.dni().trim());
        cliente.setNombre(dto.nombre().trim());
        cliente.setEmail(dto.email().trim());
        cliente.setTelefono(dto.telefono());
        cliente.setContrasena(passwordEncoder.encode(dto.contrasena()));
        cliente.setRol("CLIENTE");
        cliente.setActivo(1);
        cliente.setVerificado(0);
        cliente.setCodigoVerificacion(generarCodigo6Digitos());
        cliente.setCodigoVerificacionExpira(
                Timestamp.valueOf(LocalDateTime.now().plusMinutes(MINUTOS_VALIDEZ_CODIGO)));

        clienteRepository.save(cliente);

        boolean enviado = emailService.enviarCodigoVerificacion(
                cliente.getEmail(), cliente.getCodigoVerificacion());

        if (enviado) {
            return respuesta(true, "Registro exitoso. Se ha enviado un código de verificación a tu correo.");
        }
        if (exponerCodigoDesarrollo) {
            return new AuthResponseDTO(
                    true,
                    "Registro exitoso. El correo no está configurado; usa el código de verificación mostrado.",
                    null, null, null,
                    cliente.getCodigoVerificacion(), null, null
            );
        }
        return respuesta(true, "Registro exitoso, pero no se pudo enviar el correo de verificación. "
                + "Revisa MAIL_USERNAME y MAIL_PASSWORD en backend/.env.");
    }

    @Override
    @Transactional
    public AuthResponseDTO verificar(VerificacionDTO dto) {
        Cliente cliente = clienteRepository.findByEmail(dto.email().trim()).orElse(null);

        if (cliente == null) {
            return respuesta(false, "Email no encontrado");
        }
        if (Integer.valueOf(1).equals(cliente.getVerificado())) {
            return respuesta(true, "La cuenta ya está verificada");
        }
        if (cliente.getCodigoVerificacion() == null
                || !cliente.getCodigoVerificacion().equals(dto.codigo())) {
            return respuesta(false, "Código de verificación incorrecto o expirado");
        }
        if (cliente.getCodigoVerificacionExpira() != null
                && cliente.getCodigoVerificacionExpira().toLocalDateTime().isBefore(LocalDateTime.now())) {
            return respuesta(false, "El código de verificación expiró. Solicita uno nuevo.");
        }

        cliente.setVerificado(1);
        cliente.setCodigoVerificacion(null);
        cliente.setCodigoVerificacionExpira(null);
        clienteRepository.save(cliente);

        return respuesta(true, "Cuenta verificada correctamente");
    }

    @Override
    public AuthResponseDTO reenviarCodigo(ReenvioCodigoDTO dto) {
        Cliente cliente = clienteRepository.findByEmail(dto.email().trim()).orElse(null);

        if (cliente == null) {
            return respuesta(false, "Email no encontrado");
        }
        if (Integer.valueOf(1).equals(cliente.getVerificado())) {
            return respuesta(false, "La cuenta ya está verificada");
        }

        cliente.setCodigoVerificacion(generarCodigo6Digitos());
        cliente.setCodigoVerificacionExpira(
                Timestamp.valueOf(LocalDateTime.now().plusMinutes(MINUTOS_VALIDEZ_CODIGO)));
        clienteRepository.save(cliente);

        boolean enviado = emailService.enviarCodigoVerificacion(
                cliente.getEmail(), cliente.getCodigoVerificacion());

        if (enviado) {
            return respuesta(true, "Se ha reenviado el código de verificación a tu correo");
        }
        if (exponerCodigoDesarrollo) {
            return new AuthResponseDTO(
                    true,
                    "El correo no está configurado; usa el código de verificación mostrado.",
                    null, null, null,
                    cliente.getCodigoVerificacion(), null, null
            );
        }
        return respuesta(true, "No se pudo reenviar el correo de verificación. "
                + "Revisa MAIL_USERNAME y MAIL_PASSWORD en backend/.env.");
    }

    private AuthResponseDTO respuesta(boolean success, String message) {
        return new AuthResponseDTO(success, message, null, null, null, null, null, null);
    }

    private String generarCodigo6Digitos() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }
}
