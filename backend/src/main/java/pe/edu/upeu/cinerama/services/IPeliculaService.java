package pe.edu.upeu.cinerama.services;

import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.cinerama.dto.PeliculaDTO;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IPeliculaService {
    List<PeliculaDTO> listarTodo();
    List<PeliculaDTO> listarActivas();
    List<PeliculaDTO> listarDestacadas();
    Optional<PeliculaDTO> buscarPorId(Long id);
    PeliculaDTO crear(PeliculaDTO dto);
    Optional<PeliculaDTO> actualizar(Long id, PeliculaDTO dto);
    PeliculaDTO cambiarActivo(Long id);
    PeliculaDTO cambiarDestacado(Long id);
    String subirImagen(Long id, MultipartFile file) throws IOException;
    boolean eliminar(Long id);
}
