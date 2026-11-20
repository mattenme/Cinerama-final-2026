package pe.edu.upeu.cinerama.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.upeu.cinerama.entity.Cliente;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_CLIENTE_ID = "clienteId";
    public static final String CLAIM_NOMBRE = "nombre";

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String generateToken(Cliente cliente) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(CLAIM_ROL, cliente.getRol());
        extraClaims.put(CLAIM_CLIENTE_ID, cliente.getIdCliente());
        extraClaims.put(CLAIM_NOMBRE, cliente.getNombre());
        return buildToken(extraClaims, cliente.getDni());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRol(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_ROL, String.class));
    }

    public Long extractClienteId(String token) {
        return extractClaim(token, claims -> {
            Number id = claims.get(CLAIM_CLIENTE_ID, Number.class);
            return id == null ? null : id.longValue();
        });
    }

    public String extractNombre(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_NOMBRE, String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isTokenValid(String token) {
        try {
            return extractExpiration(token).after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long segundosRestantes(String token) {
        try {
            long milisegundos = extractExpiration(token).getTime() - System.currentTimeMillis();
            return Math.max(0, milisegundos / 1000);
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }

    public long expiracionEnSegundos() {
        return jwtExpiration / 1000;
    }

    public String renovarToken(String token) {
        Claims claims = extractAllClaims(token);
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(CLAIM_ROL, claims.get(CLAIM_ROL, String.class));
        extraClaims.put(CLAIM_CLIENTE_ID, claims.get(CLAIM_CLIENTE_ID, Number.class));
        extraClaims.put(CLAIM_NOMBRE, claims.get(CLAIM_NOMBRE, String.class));
        return buildToken(extraClaims, claims.getSubject());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
