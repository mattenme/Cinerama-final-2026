package pe.edu.upeu.cinerama.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(11)
public class MigracionProductoStock implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionProductoStock.class);

    private static final int STOCK_INICIAL = 50;

    private final JdbcTemplate jdbcTemplate;

    public MigracionProductoStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!existeColumna("PRODUCTO", "STOCK")) {
            try {
                jdbcTemplate.execute("ALTER TABLE PRODUCTO ADD (STOCK NUMBER(10))");
                log.info("Migración de dulcería: columna PRODUCTO.STOCK creada");
            } catch (DataAccessException e) {
                log.error("Migración de dulcería: no se pudo crear PRODUCTO.STOCK ({})", e.getMessage());
                return;
            }
        }

        try {
            int actualizados = jdbcTemplate.update(
                    "UPDATE PRODUCTO SET STOCK = ? WHERE STOCK IS NULL", STOCK_INICIAL);
            if (actualizados > 0) {
                log.info("Migración de dulcería: {} producto(s) con stock inicial {}", actualizados, STOCK_INICIAL);
            }
        } catch (DataAccessException e) {
            log.warn("Migración de dulcería: no se pudo inicializar el stock ({})", e.getMessage());
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
}
