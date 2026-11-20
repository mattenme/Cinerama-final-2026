package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.AsientoDTO;
import pe.edu.upeu.cinerama.entity.Asiento;
import pe.edu.upeu.cinerama.entity.Sala;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.AsientoRepository;
import pe.edu.upeu.cinerama.repository.SalaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AsientoServiceImpl implements IAsientoService {

    private final AsientoRepository asientoRepository;
    private final SalaRepository salaRepository;

    public AsientoServiceImpl(AsientoRepository asientoRepository, SalaRepository salaRepository) {
        this.asientoRepository = asientoRepository;
        this.salaRepository = salaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoDTO> listarTodo() {
        return asientoRepository.findAllConRelaciones().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoDTO> listarPorSala(Long idSala) {
        return asientoRepository.findBySalaConRelaciones(idSala).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsientoDTO> listarDisponiblesPorSala(Long idSala) {
        return asientoRepository.findAvailableBySala(idSala).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarDisponiblesPorSala(Long idSala) {
        return asientoRepository.countAvailableBySala(idSala);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AsientoDTO> buscarPorId(Long id) {
        return asientoRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public AsientoDTO crear(AsientoDTO dto) {
        Sala sala = salaRepository.findById(dto.idSala())
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada con id: " + dto.idSala()));

        Asiento asiento = new Asiento();
        asiento.setSala(sala);
        asiento.setFila(dto.fila());
        asiento.setNumero(dto.numero());
        asiento.setEstado(dto.estado() == null || dto.estado().isBlank() ? "Disponible" : dto.estado());
        return convertToDTO(asientoRepository.save(asiento));
    }

    @Override
    @Transactional
    public List<AsientoDTO> generarButacas(Long idSala, int filas, int butacasPorFila) {
        Sala sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada con id: " + idSala));

        if (filas < 1 || filas > 26 || butacasPorFila < 1 || butacasPorFila > 30) {
            throw new BadRequestException(
                    "El rango permitido es de 1 a 26 filas y de 1 a 30 butacas por fila");
        }
        if (asientoRepository.countBySala_IdSala(idSala) > 0) {
            throw new BadRequestException("La sala ya tiene butacas registradas");
        }

        List<Asiento> nuevos = new ArrayList<>();
        for (int f = 0; f < filas; f++) {
            String letra = String.valueOf((char) ('A' + f));
            for (int numero = 1; numero <= butacasPorFila; numero++) {
                Asiento asiento = new Asiento();
                asiento.setSala(sala);
                asiento.setFila(letra);
                asiento.setNumero(numero);
                asiento.setEstado("Disponible");
                nuevos.add(asiento);
            }
        }
        asientoRepository.saveAll(nuevos);

        sala.setCapacidadTotal(filas * butacasPorFila);
        salaRepository.save(sala);

        return nuevos.stream().map(this::convertToDTO).toList();
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return asientoRepository.findById(id).map(asiento -> {
            asientoRepository.delete(asiento);
            return true;
        }).orElse(false);
    }

    private AsientoDTO convertToDTO(Asiento asiento) {
        return new AsientoDTO(
                asiento.getIdAsiento(),
                asiento.getSala().getIdSala(),
                asiento.getSala().getNombre(),
                asiento.getFila(),
                asiento.getNumero(),
                asiento.getEstado()
        );
    }
}
