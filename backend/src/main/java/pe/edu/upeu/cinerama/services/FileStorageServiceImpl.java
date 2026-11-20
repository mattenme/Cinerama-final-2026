package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements IFileStorageService {

    private static final Set<String> EXTENSIONES_VALIDAS =
            Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Value("${cinerama.upload.dir:uploads}")
    private String uploadDir;

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de imágenes: " + uploadDir);
        }
    }

    @Override
    public String guardarImagen(MultipartFile file, String subDirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo no puede estar vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("El archivo debe ser una imagen (JPG, PNG, GIF, BMP o WebP)");
        }

        String extension = obtenerExtension(file.getOriginalFilename(), contentType);
        if (!EXTENSIONES_VALIDAS.contains(extension)) {
            throw new BadRequestException(
                    "Formato de imagen no soportado (" + extension + "). Usa JPG, PNG, GIF, BMP o WebP");
        }

        Path uploadPath = Paths.get(uploadDir, subDirectory);
        Files.createDirectories(uploadPath);

        Path filePath = null;
        String uniqueFilename = null;

        try (var inputStream = file.getInputStream()) {
            BufferedImage originalImage;
            try {
                originalImage = ImageIO.read(inputStream);
            } catch (IOException lectura) {
                throw new BadRequestException(
                        "El archivo no es una imagen válida o su formato no está soportado");
            }
            if (originalImage == null) {
                throw new BadRequestException(
                        "El archivo no es una imagen válida o su formato no está soportado");
            }

            boolean conTransparencia = originalImage.getColorModel().hasAlpha();
            String formatoSalida = ("png".equals(extension) || conTransparencia) ? "png" : "jpg";

            uniqueFilename = UUID.randomUUID().toString() + "." + formatoSalida;
            filePath = uploadPath.resolve(uniqueFilename);

            int maxWidth = 1200;
            int maxHeight = 1200;
            BufferedImage resizedImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, maxWidth, maxHeight);

            boolean escrita = ImageIO.write(resizedImage, formatoSalida, filePath.toFile());
            if (!escrita) {
                Files.deleteIfExists(filePath);
                throw new BadRequestException("No se pudo procesar la imagen");
            }
        } catch (BadRequestException e) {
            if (filePath != null) {
                Files.deleteIfExists(filePath);
            }
            throw e;
        } catch (IOException | RuntimeException e) {
            if (filePath != null) {
                Files.deleteIfExists(filePath);
            }
            throw e;
        }

        return "/cinerama/uploads/" + subDirectory + "/" + uniqueFilename;
    }

    private String obtenerExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
                    .toLowerCase(Locale.ROOT);
        }
        String subtipo = contentType.substring("image/".length()).toLowerCase(Locale.ROOT);
        return switch (subtipo) {
            case "jpeg", "pjpeg" -> "jpg";
            case "png" -> "png";
            default -> subtipo;
        };
    }

    @Override
    public void eliminarImagen(String ruta) throws IOException {
        if (ruta == null || ruta.isBlank()) {
            return;
        }
        String rutaRelativa = ruta.startsWith("/cinerama/uploads/") ? ruta.substring("/cinerama/uploads/".length()) : ruta;
        Path filePath = Paths.get(uploadDir, rutaRelativa);
        Files.deleteIfExists(filePath);
    }

    @Override
    public Optional<String> getRutaCompleta(String rutaRelativa) {
        Path filePath = Paths.get(uploadDir, rutaRelativa);
        return Files.exists(filePath) ? Optional.of(filePath.toAbsolutePath().toString()) : Optional.empty();
    }
}