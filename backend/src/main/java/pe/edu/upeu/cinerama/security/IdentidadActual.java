package pe.edu.upeu.cinerama.security;

import jakarta.servlet.http.HttpServletRequest;

public final class IdentidadActual {

    public static final String ATRIBUTO_CLIENTE_ID = "cinerama.clienteId";
    public static final String ATRIBUTO_ROL = "cinerama.rol";
    public static final String ATRIBUTO_NOMBRE = "cinerama.nombre";
    public static final String ATRIBUTO_TOKEN = "cinerama.jwt";

    private IdentidadActual() {
    }

    public static Long idCliente(HttpServletRequest request) {
        Object valor = request.getAttribute(ATRIBUTO_CLIENTE_ID);
        return valor instanceof Long id ? id : null;
    }

    public static String rol(HttpServletRequest request) {
        Object valor = request.getAttribute(ATRIBUTO_ROL);
        return valor == null ? null : valor.toString();
    }

    public static String nombre(HttpServletRequest request) {
        Object valor = request.getAttribute(ATRIBUTO_NOMBRE);
        return valor == null ? null : valor.toString();
    }

    public static String token(HttpServletRequest request) {
        Object valor = request.getAttribute(ATRIBUTO_TOKEN);
        return valor == null ? null : valor.toString();
    }

    public static boolean esAdmin(HttpServletRequest request) {
        return "ADMIN".equalsIgnoreCase(rol(request));
    }
}
