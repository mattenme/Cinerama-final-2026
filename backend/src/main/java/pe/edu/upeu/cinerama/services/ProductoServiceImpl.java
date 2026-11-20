package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.cinerama.dto.ProductoDTO;
import pe.edu.upeu.cinerama.entity.Producto;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.ProductoRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoServiceImpl implements IProductoService {

    private final ProductoRepository productoRepository;
    private final IFileStorageService fileStorageService;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               IFileStorageService fileStorageService) {
        this.productoRepository = productoRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> listarTodo() {
        return productoRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> listarActivos() {
        return productoRepository.findByActivo(1).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoDTO> listarPorCategoria(String categoria) {
        return productoRepository.findByCategoriaAndActivo(categoria, 1).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listarCategorias() {
        return productoRepository.findAll().stream()
                .map(Producto::getCategoria)
                .filter(categoria -> categoria != null && !categoria.isBlank())
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoDTO> buscarPorId(Long id) {
        return productoRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public ProductoDTO crear(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecio(dto.precio());
        producto.setImagenUrl(dto.imagenUrl());
        producto.setCategoria(dto.categoria());
        producto.setActivo(dto.activo() == null ? 1 : dto.activo());
        producto.setStock(dto.stock());
        return convertToDTO(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public Optional<ProductoDTO> actualizar(Long id, ProductoDTO dto) {
        return productoRepository.findById(id).map(producto -> {
            producto.setNombre(dto.nombre());
            producto.setDescripcion(dto.descripcion());
            producto.setPrecio(dto.precio());
            producto.setCategoria(dto.categoria());
            if (dto.imagenUrl() != null) {
                producto.setImagenUrl(dto.imagenUrl());
            }
            if (dto.activo() != null) {
                producto.setActivo(dto.activo());
            }
            producto.setStock(dto.stock());
            return convertToDTO(productoRepository.save(producto));
        });
    }

    @Override
    @Transactional
    public ProductoDTO cambiarActivo(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        producto.setActivo(producto.getActivo() == 1 ? 0 : 1);
        return convertToDTO(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public String subirImagen(Long id, MultipartFile file) throws IOException {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));

        String imagenUrl = fileStorageService.guardarImagen(file, "productos");
        String anterior = producto.getImagenUrl();

        producto.setImagenUrl(imagenUrl);
        productoRepository.save(producto);

        if (anterior != null && !anterior.isBlank()) {
            try {
                fileStorageService.eliminarImagen(anterior);
            } catch (IOException ignored) {
            }
        }
        return imagenUrl;
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return productoRepository.findById(id).map(producto -> {
            String imagen = producto.getImagenUrl();
            productoRepository.delete(producto);

            if (imagen != null && !imagen.isBlank()) {
                try {
                    fileStorageService.eliminarImagen(imagen);
                } catch (IOException ignored) {
                }
            }
            return true;
        }).orElse(false);
    }

    private ProductoDTO convertToDTO(Producto producto) {
        return new ProductoDTO(
                producto.getIdProducto(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getImagenUrl(),
                producto.getCategoria(),
                producto.getActivo(),
                producto.getStock()
        );
    }
}
