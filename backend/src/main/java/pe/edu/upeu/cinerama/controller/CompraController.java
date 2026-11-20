package pe.edu.upeu.cinerama.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.cinerama.dto.CompraDTO;
import pe.edu.upeu.cinerama.dto.CompraRequestDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.security.IdentidadActual;
import pe.edu.upeu.cinerama.services.ICompraService;
import pe.edu.upeu.cinerama.services.IQRService;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/compras")
public class CompraController {

    private final ICompraService compraService;
    private final IQRService qrService;

    public CompraController(ICompraService compraService, IQRService qrService) {
        this.compraService = compraService;
        this.qrService = qrService;
    }

    @GetMapping
    public ResponseEntity<List<CompraDTO>> listar() {
        return ResponseEntity.ok(compraService.listarTodo());
    }

    @GetMapping("/mias")
    public ResponseEntity<List<CompraDTO>> listarMias(HttpServletRequest request) {
        Long idCliente = IdentidadActual.idCliente(request);
        return ResponseEntity.ok(compraService.listarPorCliente(idCliente));
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<CompraDTO>> listarPorCliente(@PathVariable Long idCliente) {
        return ResponseEntity.ok(compraService.listarPorCliente(idCliente));
    }

    @GetMapping("/funcion/{idFuncion}/confirmadas")
    public ResponseEntity<List<CompraDTO>> listarConfirmadasPorFuncion(@PathVariable Long idFuncion) {
        return ResponseEntity.ok(compraService.listarConfirmadasPorFuncion(idFuncion));
    }

    @GetMapping("/funcion/{idFuncion}/asientos-ocupados")
    public ResponseEntity<List<Long>> listarAsientosOcupados(@PathVariable Long idFuncion) {
        return ResponseEntity.ok(compraService.listarAsientosOcupados(idFuncion));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraDTO> obtener(@PathVariable Long id) {
        return compraService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));
    }

    @GetMapping(value = "/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> obtenerQr(@PathVariable Long id) {
        CompraDTO compra = compraService.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada con id: " + id));

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(qrService.generarBytes(compra.codigoQr()));
    }

    @PostMapping("/procesar")
    public ResponseEntity<List<CompraDTO>> procesar(@Valid @RequestBody CompraRequestDTO dto,
                                                    HttpServletRequest request) {
        Long idCliente = IdentidadActual.idCliente(request);
        return new ResponseEntity<>(compraService.procesarCompra(dto, idCliente), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<CompraDTO> cancelar(@PathVariable Long id, HttpServletRequest request) {
        return compraService.cancelar(id, IdentidadActual.esAdmin(request))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo cancelar con id: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CompraDTO> eliminar(@PathVariable Long id, HttpServletRequest request) {
        return compraService.cancelar(id, IdentidadActual.esAdmin(request))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No se pudo cancelar con id: " + id));
    }
}
