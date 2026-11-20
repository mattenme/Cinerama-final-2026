package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.cinerama.dto.PeliculaDTO;
import pe.edu.upeu.cinerama.entity.Pelicula;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.PeliculaRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class PeliculaServiceImpl implements IPeliculaService {

    private static final List<String> GENEROS = List.of(
            "Acción", "Animación", "Aventura", "Ciencia ficción", "Comedia", "Documental",
            "Drama", "Familiar", "Fantasía", "Musical", "Romance", "Suspenso", "Terror");

    private final PeliculaRepository peliculaRepository;
    private final IFileStorageService fileStorageService;

    public PeliculaServiceImpl(PeliculaRepository peliculaRepository,
                               IFileStorageService fileStorageService) {
        this.peliculaRepository = peliculaRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeliculaDTO> listarTodo() {
        return peliculaRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeliculaDTO> listarActivas() {
        return peliculaRepository.findAllActive().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeliculaDTO> listarDestacadas() {
        return peliculaRepository.findByDestacadoAndActivo(1, 1).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PeliculaDTO> buscarPorId(Long id) {
        return peliculaRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional
    public PeliculaDTO crear(PeliculaDTO dto) {
        Pelicula pelicula = new Pelicula();
        pelicula.setTitulo(dto.titulo());
        pelicula.setDuracionMinutos(dto.duracionMinutos());
        pelicula.setGenero(normalizarGenero(dto.genero(), null));
        pelicula.setSinopsis(dto.sinopsis());
        pelicula.setImagenUrl(dto.imagenUrl());
        pelicula.setDestacado(dto.destacado() == null ? 0 : dto.destacado());
        pelicula.setActivo(dto.activo() == null ? 1 : dto.activo());
        return convertToDTO(peliculaRepository.save(pelicula));
    }

    @Override
    @Transactional
    public Optional<PeliculaDTO> actualizar(Long id, PeliculaDTO dto) {
        return peliculaRepository.findById(id).map(pelicula -> {
            pelicula.setTitulo(dto.titulo());
            pelicula.setDuracionMinutos(dto.duracionMinutos());
            pelicula.setGenero(normalizarGenero(dto.genero(), pelicula.getGenero()));
            pelicula.setSinopsis(dto.sinopsis());
            if (dto.imagenUrl() != null) {
                pelicula.setImagenUrl(dto.imagenUrl());
            }
            if (dto.destacado() != null) {
                pelicula.setDestacado(dto.destacado());
            }
            if (dto.activo() != null) {
                pelicula.setActivo(dto.activo());
            }
            return convertToDTO(peliculaRepository.save(pelicula));
        });
    }

    private String normalizarGenero(String genero, String actual) {
        if (genero == null || genero.isBlank()) {
            return null;
        }
        String texto = genero.trim();
        if (actual != null && actual.equalsIgnoreCase(texto)) {
            return actual;
        }
        return GENEROS.stream()
                .filter((permitido) -> permitido.equalsIgnoreCase(texto))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Género no válido: " + texto + ". Elige uno de los géneros del formulario."));
    }

    @Override
    @Transactional
    public PeliculaDTO cambiarActivo(Long id) {
        Pelicula pelicula = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con id: " + id));
        pelicula.setActivo(pelicula.getActivo() == 1 ? 0 : 1);
        return convertToDTO(peliculaRepository.save(pelicula));
    }

    @Override
    @Transactional
    public PeliculaDTO cambiarDestacado(Long id) {
        Pelicula pelicula = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con id: " + id));
        pelicula.setDestacado(pelicula.getDestacado() == 1 ? 0 : 1);
        return convertToDTO(peliculaRepository.save(pelicula));
    }

    @Override
    @Transactional
    public String subirImagen(Long id, MultipartFile file) throws IOException {
        Pelicula pelicula = peliculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Película no encontrada con id: " + id));

        String imagenUrl = fileStorageService.guardarImagen(file, "peliculas");
        String anterior = pelicula.getImagenUrl();

        pelicula.setImagenUrl(imagenUrl);
        peliculaRepository.save(pelicula);

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
        return peliculaRepository.findById(id).map(pelicula -> {
            String imagen = pelicula.getImagenUrl();
            peliculaRepository.delete(pelicula);

            if (imagen != null && !imagen.isBlank()) {
                try {
                    fileStorageService.eliminarImagen(imagen);
                } catch (IOException ignored) {
                }
            }
            return true;
        }).orElse(false);
    }

    private PeliculaDTO convertToDTO(Pelicula pelicula) {
        return new PeliculaDTO(
                pelicula.getIdPelicula(),
                pelicula.getTitulo(),
                pelicula.getDuracionMinutos(),
                pelicula.getGenero(),
                pelicula.getSinopsis(),
                pelicula.getImagenUrl(),
                pelicula.getDestacado(),
                pelicula.getActivo()
        );
    }
}
