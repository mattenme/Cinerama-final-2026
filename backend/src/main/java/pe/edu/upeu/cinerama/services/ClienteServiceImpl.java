package pe.edu.upeu.cinerama.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.dto.ClienteDTO;
import pe.edu.upeu.cinerama.entity.Cliente;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.ClienteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteServiceImpl implements IClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteDTO> listarTodo() {
        return clienteRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteDTO> buscarPorId(Long id) {
        return clienteRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorEmail(String email) {
        return clienteRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorDni(String dni) {
        return clienteRepository.existsByDni(dni);
    }

    @Override
    @Transactional
    public ClienteDTO cambiarActivo(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        cliente.setActivo(cliente.getActivo() == 1 ? 0 : 1);
        return convertToDTO(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return clienteRepository.findById(id).map(cliente -> {
            clienteRepository.delete(cliente);
            return true;
        }).orElse(false);
    }

    private ClienteDTO convertToDTO(Cliente cliente) {
        return new ClienteDTO(
                cliente.getIdCliente(),
                cliente.getDni(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getRol(),
                cliente.getActivo(),
                cliente.getVerificado()
        );
    }
}
