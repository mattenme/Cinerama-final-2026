package pe.edu.upeu.cinerama.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.cinerama.dto.ClienteVsPromedioDTO;
import pe.edu.upeu.cinerama.dto.ComparacionPeriodoDTO;
import pe.edu.upeu.cinerama.dto.OcupacionFuncionDTO;
import pe.edu.upeu.cinerama.dto.ResumenFuncionDTO;
import pe.edu.upeu.cinerama.dto.TopClienteDTO;
import pe.edu.upeu.cinerama.dto.VentaPeliculaDTO;
import pe.edu.upeu.cinerama.services.ReporteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/funcion/{idFuncion}/resumen")
    public ResponseEntity<ResumenFuncionDTO> resumenFuncion(@PathVariable Long idFuncion) {
        return ResponseEntity.ok(reporteService.resumenFuncion(idFuncion));
    }

    @GetMapping("/ventas-peliculas")
    public ResponseEntity<List<VentaPeliculaDTO>> ventasPorPelicula() {
        return ResponseEntity.ok(reporteService.ventasPorPelicula());
    }

    @GetMapping("/ocupacion")
    public ResponseEntity<List<OcupacionFuncionDTO>> ocupacion() {
        return ResponseEntity.ok(reporteService.ocupacionPorFuncion());
    }

    @GetMapping("/top-clientes")
    public ResponseEntity<List<TopClienteDTO>> topClientes(
            @RequestParam(name = "limite", defaultValue = "5") Integer limite) {
        return ResponseEntity.ok(reporteService.topClientes(limite));
    }

    @GetMapping("/comparacion")
    public ResponseEntity<List<ComparacionPeriodoDTO>> comparacion(
            @RequestParam(name = "dias", defaultValue = "30") Integer dias) {
        return ResponseEntity.ok(reporteService.comparacionPeriodos(dias));
    }

    @GetMapping("/cliente/{idCliente}/comparacion")
    public ResponseEntity<ClienteVsPromedioDTO> clienteVsPromedio(@PathVariable Long idCliente) {
        return ResponseEntity.ok(reporteService.clienteVsPromedio(idCliente));
    }
}
