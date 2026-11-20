package pe.edu.upeu.cinerama.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.entity.Compra;
import pe.edu.upeu.cinerama.repository.CompraRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LimpiezaReservasService {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaReservasService.class);

    private final CompraRepository compraRepository;

    @Value("${cinerama.compras.reserva-minutos:15}")
    private int minutosReserva;

    public LimpiezaReservasService(CompraRepository compraRepository) {
        this.compraRepository = compraRepository;
    }

    @Scheduled(
            initialDelayString = "${cinerama.compras.limpieza-inicial-ms:30000}",
            fixedDelayString = "${cinerama.compras.limpieza-ms:60000}")
    @Transactional
    public void liberarReservasVencidas() {
        Timestamp limite = Timestamp.valueOf(LocalDateTime.now().minusMinutes(minutosReserva));
        List<Compra> pendientes = compraRepository.findPendientesAnterioresA(limite);
        if (pendientes.isEmpty()) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        for (Compra compra : pendientes) {
            compra.setEstado(Compra.ESTADO_CANCELADA);
            compra.setFechaCancelacion(Timestamp.valueOf(ahora));
            compra.getDetalles().clear();
        }
        compraRepository.saveAll(pendientes);

        log.info("Reservas liberadas: {} compra(s) PENDIENTE con más de {} min",
                pendientes.size(), minutosReserva);
    }
}
