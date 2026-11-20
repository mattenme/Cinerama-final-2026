package pe.edu.upeu.cinerama.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7).trim();

        if (SecurityContextHolder.getContext().getAuthentication() == null && jwtService.isTokenValid(jwt)) {
            try {
                Long clienteId = jwtService.extractClienteId(jwt);
                String rol = jwtService.extractRol(jwt);

                request.setAttribute(IdentidadActual.ATRIBUTO_CLIENTE_ID, clienteId);
                request.setAttribute(IdentidadActual.ATRIBUTO_ROL, rol);
                request.setAttribute(IdentidadActual.ATRIBUTO_NOMBRE, jwtService.extractNombre(jwt));
                request.setAttribute(IdentidadActual.ATRIBUTO_TOKEN, jwt);

                if (clienteId != null) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            jwtService.extractUsername(jwt),
                            null,
                            rol == null ? List.of() : List.of(new SimpleGrantedAuthority(rol))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (JwtException | IllegalArgumentException e) {

                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
