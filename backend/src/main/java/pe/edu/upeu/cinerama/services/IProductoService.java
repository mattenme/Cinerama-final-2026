package pe.edu.upeu.cinerama.services;

import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.cinerama.dto.ProductoDTO;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface IProductoService {
    List<ProductoDTO> listarTodo();
    List<ProductoDTO> listarActivos();
    List<ProductoDTO> listarPorCategoria(String categoria);
    List<String> listarCategorias();
    Optional<ProductoDTO> buscarPorId(Long id);
    ProductoDTO crear(ProductoDTO dto);
    Optional<ProductoDTO> actualizar(Long id, ProductoDTO dto);
    ProductoDTO cambiarActivo(Long id);
    String subirImagen(Long id, MultipartFile file) throws IOException;
    boolean eliminar(Long id);
}
