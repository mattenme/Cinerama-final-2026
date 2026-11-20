package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.ValidacionDTO;

import java.util.List;

public interface IPuertaService {

    ValidacionDTO validar(String codigo, Long idEmpleado);

    List<ValidacionDTO> listarRecientes();
}
