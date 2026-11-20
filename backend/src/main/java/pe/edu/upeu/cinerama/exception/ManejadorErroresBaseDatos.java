package pe.edu.upeu.cinerama.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ManejadorErroresBaseDatos {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresBaseDatos.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> errorBaseDatos(DataAccessException ex,
                                                              HttpServletRequest request) {
        log.error("Error de base de datos en {} {}", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "message", "No se pudo completar la operación en la base de datos"
                ));
    }
}
