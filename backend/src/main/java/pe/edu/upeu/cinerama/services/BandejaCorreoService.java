package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import pe.edu.upeu.cinerama.dto.CorreoEnviadoDTO;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class BandejaCorreoService {

    private static final int LIMITE = 25;

    private final Deque<CorreoEnviadoDTO> correos = new ConcurrentLinkedDeque<>();

    public void registrar(String destino, String asunto, boolean enviado, String detalle,
                          String codigoQr, String html) {
        correos.addFirst(new CorreoEnviadoDTO(
                LocalDateTime.now(), destino, asunto, enviado, detalle, codigoQr, html));

        while (correos.size() > LIMITE) {
            correos.removeLast();
        }
    }

    public List<CorreoEnviadoDTO> listar() {
        return List.copyOf(correos);
    }

    public int limpiar() {
        int total = correos.size();
        correos.clear();
        return total;
    }
}
