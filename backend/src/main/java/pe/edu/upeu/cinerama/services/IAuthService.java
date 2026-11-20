package pe.edu.upeu.cinerama.services;

import pe.edu.upeu.cinerama.dto.AuthResponseDTO;
import pe.edu.upeu.cinerama.dto.LoginDTO;
import pe.edu.upeu.cinerama.dto.ReenvioCodigoDTO;
import pe.edu.upeu.cinerama.dto.RegistroDTO;
import pe.edu.upeu.cinerama.dto.VerificacionDTO;

public interface IAuthService {
    AuthResponseDTO login(LoginDTO dto);
    AuthResponseDTO registrar(RegistroDTO dto);
    AuthResponseDTO verificar(VerificacionDTO dto);
    AuthResponseDTO reenviarCodigo(ReenvioCodigoDTO dto);
}
