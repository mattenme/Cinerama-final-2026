package pe.edu.upeu.cinerama.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class MigracionCompraDetalle implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionCompraDetalle.class);

    private final JdbcTemplate jdbcTemplate;

    public MigracionCompraDetalle(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        permitirCabeceraSinAsiento();

        if (!existeColumna("COMPRA", "ID_ASIENTO")) {
            return;
        }

        int movidas = copiarButacasHistoricas();
        if (movidas > 0) {
            log.info("Migración de compras: {} butaca(s) históricas pasadas de COMPRA a COMPRA_DETALLE", movidas);
        }
    }

    private void permitirCabeceraSinAsiento() {
        try {
            String nullable = jdbcTemplate.queryForObject(
                    "SELECT NULLABLE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'COMPRA' AND COLUMN_NAME = 'ID_ASIENTO'",
                    String.class);

            if ("N".equalsIgnoreCase(nullable)) {
                jdbcTemplate.execute("ALTER TABLE COMPRA MODIFY (ID_ASIENTO NULL)");
                log.info("Migración de compras: COMPRA.ID_ASIENTO ahora admite nulos");
            }
        } catch (EmptyResultDataAccessException e) {
        } catch (DataAccessException e) {
            log.warn("Migración de compras: no se pudo comprobar COMPRA.ID_ASIENTO ({})", e.getMessage());
        }
    }

    private boolean existeColumna(String tabla, String columna) {
        try {
            Integer total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, tabla, columna);
            return total != null && total > 0;
        } catch (DataAccessException e) {
            return false;
        }
    }

    private int copiarButacasHistoricas() {
        String sql = """
                INSERT INTO COMPRA_DETALLE (ID_COMPRA, ID_FUNCION, ID_ASIENTO, PRECIO)
                SELECT ID_COMPRA, ID_FUNCION, ID_ASIENTO, PRECIO
                FROM (
                    SELECT c.ID_COMPRA, c.ID_FUNCION, c.ID_ASIENTO, c.PRECIO,
                           ROW_NUMBER() OVER (PARTITION BY c.ID_FUNCION, c.ID_ASIENTO
                                              ORDER BY c.ID_COMPRA) AS RN
                    FROM COMPRA c
                    WHERE c.ID_ASIENTO IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM COMPRA_DETALLE d WHERE d.ID_COMPRA = c.ID_COMPRA)
                )
                WHERE RN = 1
                """;

        try {
            return jdbcTemplate.update(sql);
        } catch (DataAccessException e) {
            log.error("Migración de compras: no se pudieron copiar las butacas históricas ({})", e.getMessage());
            return 0;
        }
    }
}
