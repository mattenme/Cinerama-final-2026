package pe.edu.upeu.cinerama.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pe.edu.upeu.cinerama.repository.CompraRepository;
import pe.edu.upeu.cinerama.security.IdentidadActual;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AutorizacionInterceptor implements HandlerInterceptor {

    private static final String ADMIN = "ADMIN";
    private static final String EMPLEADO = "EMPLEADO";

    private enum Nivel { PUBLICO, AUTENTICADO, PROPIETARIO, PUERTA, ADMIN }

    private record Regla(String metodo, Pattern patron, Nivel nivel, int grupoPropietario) {
        Regla(String metodo, String regex, Nivel nivel) {
            this(metodo, Pattern.compile(regex), nivel, 0);
        }

        Regla(String metodo, String regex, Nivel nivel, int grupoPropietario) {
            this(metodo, Pattern.compile(regex), nivel, grupoPropietario);
        }
    }

    private static final List<Regla> REGLAS = List.of(
            new Regla(null, "^/api/v1/auth/(login|register|verificar|reenviar-codigo|check|logout|renovar)$",
                    Nivel.PUBLICO),
            new Regla("GET", "^/api/v1/(peliculas|salas|funciones|asientos|productos)(/.*)?$",
                    Nivel.PUBLICO),
            new Regla("GET", "^/api/v1/promociones/(activas|codigo/[^/]+)$", Nivel.PUBLICO),
            new Regla("GET", "^/api/v1/calificaciones/pelicula/\\d+(/promedio)?$", Nivel.PUBLICO),
            new Regla("GET", "^/api/v1/compras/funcion/\\d+/asientos-ocupados$", Nivel.PUBLICO),

            new Regla("GET", "^/api/v1/compras/mias$", Nivel.AUTENTICADO),
            new Regla("GET", "^/api/v1/calificaciones/pelicula/\\d+/mia$", Nivel.AUTENTICADO),
            new Regla("GET", "^/api/v1/calificaciones/pelicula/\\d+/puede-calificar$", Nivel.AUTENTICADO),
            new Regla("GET", "^/api/v1/incidencias/mias$", Nivel.AUTENTICADO),

            new Regla("GET", "^/api/v1/compras/cliente/(\\d+)$", Nivel.PROPIETARIO, 1),
            new Regla("GET", "^/api/v1/compras/(\\d+)$", Nivel.PROPIETARIO),
            new Regla("GET", "^/api/v1/compras/(\\d+)/qr\\.png$", Nivel.PROPIETARIO),
            new Regla("GET", "^/api/v1/calificaciones/cliente/(\\d+)$", Nivel.PROPIETARIO, 1),
            new Regla("GET", "^/api/v1/calificaciones/(\\d+)/(\\d+)$", Nivel.PROPIETARIO, 2),
            new Regla("DELETE", "^/api/v1/calificaciones/(\\d+)/(\\d+)$", Nivel.PROPIETARIO, 2),
            new Regla("GET", "^/api/v1/incidencias/cliente/(\\d+)$", Nivel.PROPIETARIO, 1),

            new Regla("POST", "^/api/v1/compras/(\\d+)/cancelar$", Nivel.PROPIETARIO),
            new Regla("DELETE", "^/api/v1/compras/(\\d+)$", Nivel.PROPIETARIO),

            new Regla(null, "^/api/v1/puerta(/.*)?$", Nivel.PUERTA),

            new Regla("POST", "^/api/v1/compras/procesar$", Nivel.AUTENTICADO),
            new Regla("POST", "^/api/v1/calificaciones$", Nivel.AUTENTICADO),
            new Regla("POST", "^/api/v1/incidencias$", Nivel.AUTENTICADO)
    );

    private final CompraRepository compraRepository;

    public AutorizacionInterceptor(CompraRepository compraRepository) {
        this.compraRepository = compraRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String ruta = request.getRequestURI().substring(request.getContextPath().length());

        Regla aplicable = null;
        Matcher coincidencia = null;
        for (Regla regla : REGLAS) {
            if (regla.metodo() != null && !regla.metodo().equalsIgnoreCase(request.getMethod())) {
                continue;
            }
            Matcher matcher = regla.patron().matcher(ruta);
            if (matcher.matches()) {
                aplicable = regla;
                coincidencia = matcher;
                break;
            }
        }

        if (aplicable == null) {
            return exigirAdmin(request, response);
        }

        return switch (aplicable.nivel()) {
            case PUBLICO -> true;
            case AUTENTICADO -> exigirAutenticacion(request, response);
            case PUERTA -> exigirPuerta(request, response);
            case ADMIN -> exigirAdmin(request, response);
            case PROPIETARIO -> exigirPropietario(request, response, aplicable, coincidencia);
        };
    }

    private boolean exigirAutenticacion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (idCliente(request) != null) {
            return true;
        }
        return negar(response, 401, "Debes iniciar sesión para realizar esta operación");
    }

    private boolean exigirAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (ADMIN.equalsIgnoreCase(rol(request))) {
            return true;
        }
        if (idCliente(request) == null) {
            return negar(response, 401, "Debes iniciar sesión para realizar esta operación");
        }
        return negar(response, 403, "Esta operación requiere permisos de administrador");
    }

    private boolean exigirPuerta(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String rol = rol(request);
        if (ADMIN.equalsIgnoreCase(rol) || EMPLEADO.equalsIgnoreCase(rol)) {
            return true;
        }
        if (idCliente(request) == null) {
            return negar(response, 401, "Debes iniciar sesión para usar el módulo de puerta");
        }
        return negar(response, 403, "Esta operación requiere rol EMPLEADO o ADMIN");
    }

    private boolean exigirPropietario(HttpServletRequest request, HttpServletResponse response,
                                      Regla regla, Matcher coincidencia) throws IOException {

        if (ADMIN.equalsIgnoreCase(rol(request))) {
            return true;
        }

        Long cliente = idCliente(request);
        if (cliente == null) {
            return negar(response, 401, "Debes iniciar sesión para consultar esta información");
        }

        if (regla.grupoPropietario() == 0) {
            Long idCompra = parseId(coincidencia.group(1));
            Optional<Long> dueno = idCompra == null
                    ? Optional.empty()
                    : compraRepository.buscarIdClientePorCompra(idCompra);

            if (dueno.isPresent() && Objects.equals(dueno.get(), cliente)) {
                return true;
            }
            return negar(response, 403, "Esta compra pertenece a otro cliente");
        }

        Long solicitado = parseId(coincidencia.group(regla.grupoPropietario()));
        if (Objects.equals(solicitado, cliente)) {
            return true;
        }
        return negar(response, 403, "Solo puedes consultar tu propia información");
    }

    private Long idCliente(HttpServletRequest request) {
        return IdentidadActual.idCliente(request);
    }

    private String rol(HttpServletRequest request) {
        return IdentidadActual.rol(request);
    }

    private Long parseId(String valor) {
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean negar(HttpServletResponse response, int estado, String mensaje) throws IOException {
        response.setStatus(estado);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + estado + ",\"message\":\"" + mensaje + "\"}");
        return false;
    }
}
