package pe.edu.upeu.cinerama.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.cinerama.dto.FuncionDTO;
import pe.edu.upeu.cinerama.dto.FuncionRequestDTO;
import pe.edu.upeu.cinerama.entity.Funcion;
import pe.edu.upeu.cinerama.entity.Pelicula;
import pe.edu.upeu.cinerama.entity.Sala;
import pe.edu.upeu.cinerama.exception.BadRequestException;
import pe.edu.upeu.cinerama.repository.FuncionRepository;
import pe.edu.upeu.cinerama.repository.PeliculaRepository;
import pe.edu.upeu.cinerama.repository.SalaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FuncionServiceImplTest {

    private static final BigDecimal PRECIO = new BigDecimal("12.50");

    @Mock
    private FuncionRepository funcionRepository;
    @Mock
    private PeliculaRepository peliculaRepository;
    @Mock
    private SalaRepository salaRepository;

    @InjectMocks
    private FuncionServiceImpl service;

    private Sala sala;
    private Pelicula pelicula;

    @BeforeEach
    void setUp() {
        sala = new Sala();
        sala.setIdSala(1L);
        sala.setNombre("Sala 1");

        pelicula = new Pelicula();
        pelicula.setIdPelicula(1L);
        pelicula.setTitulo("Dune");
        pelicula.setDuracionMinutos(120);
    }

    /** Fecha relativa para que las pruebas no dependan del día en que se ejecutan. */
    private LocalDateTime inicioFuturo() {
        return LocalDateTime.now().plusDays(7)
                .withHour(19).withMinute(0).withSecond(0).withNano(0);
    }

    private Funcion funcionExistente(LocalDateTime inicio) {
        Funcion funcion = new Funcion();
        funcion.setIdFuncion(9L);
        funcion.setSala(sala);
        funcion.setPelicula(pelicula);
        funcion.setHoraInicio(inicio);
        funcion.setActivo(1);
        return funcion;
    }

    @Test
    void rechazaFuncionQueSePisaConOtraDeLaMismaSala() {
        LocalDateTime inicio = inicioFuturo();
        given(peliculaRepository.findById(1L)).willReturn(Optional.of(pelicula));
        given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
        given(funcionRepository.findActivasEnRango(eq(1L), any(), any()))
                .willReturn(List.of(funcionExistente(inicio.minusMinutes(30))));

        FuncionRequestDTO request = new FuncionRequestDTO(1L, 1L, inicio, "PROGRAMADA", 1, PRECIO);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ya tiene");

        verify(funcionRepository, never()).save(any());
    }

    @Test
    void permiteFuncionCuandoElHorarioEstaLibre() {
        LocalDateTime inicio = inicioFuturo();
        given(peliculaRepository.findById(1L)).willReturn(Optional.of(pelicula));
        given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
        given(funcionRepository.findActivasEnRango(eq(1L), any(), any()))
                .willReturn(List.of(funcionExistente(inicio.minusMinutes(300))));

        AtomicReference<Funcion> guardada = new AtomicReference<>();
        given(funcionRepository.save(any(Funcion.class))).willAnswer(invocacion -> {
            Funcion f = invocacion.getArgument(0);
            f.setIdFuncion(10L);
            guardada.set(f);
            return f;
        });
        given(funcionRepository.findByIdWithRelations(10L))
                .willAnswer(invocacion -> Optional.of(guardada.get()));

        FuncionRequestDTO request = new FuncionRequestDTO(1L, 1L, inicio, "PROGRAMADA", 1, PRECIO);

        FuncionDTO resultado = service.crear(request);

        assertThat(resultado.horaInicio()).isEqualTo(inicio);
        assertThat(resultado.idSala()).isEqualTo(1L);
        assertThat(resultado.precio()).isEqualByComparingTo(PRECIO);
        verify(funcionRepository).save(any(Funcion.class));
    }

    @Test
    void alEditarNoSeComparaConsigoMisma() {
        LocalDateTime inicio = inicioFuturo();
        Funcion propia = funcionExistente(inicio);
        given(funcionRepository.findByIdWithRelations(9L)).willReturn(Optional.of(propia));
        given(peliculaRepository.findById(1L)).willReturn(Optional.of(pelicula));
        given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
        given(funcionRepository.findActivasEnRango(eq(1L), any(), any())).willReturn(List.of(propia));
        given(funcionRepository.save(any(Funcion.class))).willAnswer(i -> i.getArgument(0));

        FuncionRequestDTO request = new FuncionRequestDTO(1L, 1L, inicio, "PROGRAMADA", 1, PRECIO);

        FuncionDTO resultado = service.actualizar(9L, request).orElseThrow();

        assertThat(resultado.idFuncion()).isEqualTo(9L);
        assertThat(resultado.estado()).isEqualTo("PROGRAMADA");
    }

    @Test
    void rechazaFuncionEnFechaPasada() {
        LocalDateTime inicio = LocalDateTime.now().minusDays(2)
                .withHour(19).withMinute(0).withSecond(0).withNano(0);
        given(peliculaRepository.findById(1L)).willReturn(Optional.of(pelicula));
        given(salaRepository.findById(1L)).willReturn(Optional.of(sala));

        FuncionRequestDTO request = new FuncionRequestDTO(1L, 1L, inicio, null, 1, PRECIO);

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("pasada");

        verify(funcionRepository, never()).save(any());
    }

    @Test
    void permiteEditarUnaFuncionPasadaSinCambiarSuFecha() {
        LocalDateTime inicio = LocalDateTime.now().minusDays(3)
                .withHour(19).withMinute(0).withSecond(0).withNano(0);
        // La BD guarda segundos: editar sin tocar la hora debe seguir permitido.
        Funcion propia = funcionExistente(inicio.plusSeconds(25));
        given(funcionRepository.findByIdWithRelations(9L)).willReturn(Optional.of(propia));
        given(peliculaRepository.findById(1L)).willReturn(Optional.of(pelicula));
        given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
        given(funcionRepository.findActivasEnRango(eq(1L), any(), any())).willReturn(List.of(propia));
        given(funcionRepository.save(any(Funcion.class))).willAnswer(i -> i.getArgument(0));

        FuncionRequestDTO request = new FuncionRequestDTO(1L, 1L, inicio, null, 1, PRECIO);

        FuncionDTO resultado = service.actualizar(9L, request).orElseThrow();

        assertThat(resultado.estado()).isEqualTo("FINALIZADA");
    }
}
