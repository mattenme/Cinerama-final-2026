package pe.edu.upeu.cinerama.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.cinerama.entity.Asiento;
import pe.edu.upeu.cinerama.entity.Cliente;
import pe.edu.upeu.cinerama.entity.Funcion;
import pe.edu.upeu.cinerama.entity.Pelicula;
import pe.edu.upeu.cinerama.entity.Producto;
import pe.edu.upeu.cinerama.entity.Promocion;
import pe.edu.upeu.cinerama.entity.Sala;
import pe.edu.upeu.cinerama.repository.AsientoRepository;
import pe.edu.upeu.cinerama.repository.ClienteRepository;
import pe.edu.upeu.cinerama.repository.FuncionRepository;
import pe.edu.upeu.cinerama.repository.PeliculaRepository;
import pe.edu.upeu.cinerama.repository.ProductoRepository;
import pe.edu.upeu.cinerama.repository.PromocionRepository;
import pe.edu.upeu.cinerama.repository.SalaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "cinerama.seed", name = "demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String[] FILAS = {"A", "B", "C", "D", "E", "F"};
    private static final LocalTime[] HORARIOS = {
            LocalTime.of(14, 0), LocalTime.of(17, 30), LocalTime.of(20, 30)
    };

    private final SalaRepository salaRepository;
    private final AsientoRepository asientoRepository;
    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;
    private final FuncionRepository funcionRepository;
    private final PeliculaRepository peliculaRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${cinerama.seed.admin-dni:00000001}")
    private String adminDni;

    @Value("${cinerama.seed.admin-password:admin123}")
    private String adminPassword;

    @Value("${cinerama.seed.admin-reset-password:false}")
    private boolean resetAdminPassword;

    public DemoDataInitializer(SalaRepository salaRepository,
                               AsientoRepository asientoRepository,
                               ProductoRepository productoRepository,
                               PromocionRepository promocionRepository,
                               FuncionRepository funcionRepository,
                               PeliculaRepository peliculaRepository,
                               ClienteRepository clienteRepository,
                               PasswordEncoder passwordEncoder) {
        this.salaRepository = salaRepository;
        this.asientoRepository = asientoRepository;
        this.productoRepository = productoRepository;
        this.promocionRepository = promocionRepository;
        this.funcionRepository = funcionRepository;
        this.peliculaRepository = peliculaRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearAdminDemo();
        crearClienteDemo();
        crearEmpleadoDemo();
        List<Sala> salas = crearSalas();
        crearProductos();
        crearPromociones();
        crearFunciones(salas);
        completarPreciosDeFunciones();
    }

    private void crearEmpleadoDemo() {
        if (clienteRepository.findByDni("11223344").isPresent()) {
            return;
        }
        Cliente empleado = new Cliente();
        empleado.setDni("11223344");
        empleado.setNombre("Empleado de Puerta");
        empleado.setEmail("puerta@cinerama.pe");
        empleado.setTelefono("999777666");
        empleado.setContrasena(passwordEncoder.encode("empleado123"));
        empleado.setRol("EMPLEADO");
        empleado.setActivo(1);
        empleado.setVerificado(1);
        clienteRepository.save(empleado);
        log.info("Datos demo: cuenta de empleado creada");
    }

    private void crearAdminDemo() {
        Cliente admin = clienteRepository.findByDni(adminDni).orElse(null);

        if (admin == null) {
            admin = new Cliente();
            admin.setDni(adminDni);
            admin.setNombre("Administrador");
            admin.setEmail("admin@cinerama.pe");
            admin.setTelefono("999000111");
            admin.setContrasena(passwordEncoder.encode(adminPassword));
            admin.setRol("ADMIN");
            admin.setActivo(1);
            admin.setVerificado(1);
            clienteRepository.save(admin);
            log.info("Datos demo: cuenta de administrador creada (cambia la contraseña en producción)");
            return;
        }

        boolean cambios = false;
        if (!"ADMIN".equalsIgnoreCase(admin.getRol())) {
            admin.setRol("ADMIN");
            cambios = true;
        }
        if (!Integer.valueOf(1).equals(admin.getVerificado())) {
            admin.setVerificado(1);
            cambios = true;
        }
        if (!Integer.valueOf(1).equals(admin.getActivo())) {
            admin.setActivo(1);
            cambios = true;
        }

        if (resetAdminPassword) {
            admin.setContrasena(passwordEncoder.encode(adminPassword));
            admin.setIntentosFallidos(0);
            admin.setBloqueadoHasta(null);
            cambios = true;
            log.warn("Datos demo: contraseña de administrador restaurada y cuenta desbloqueada (CINERAMA_ADMIN_RESET)");
        }

        if (cambios) {
            clienteRepository.save(admin);
            log.info("Datos demo: cuenta de administrador habilitada como ADMIN verificada");
        }
    }

    private void crearClienteDemo() {
        if (clienteRepository.findByDni("77889901").isPresent()) {
            return;
        }
        Cliente cliente = new Cliente();
        cliente.setDni("77889901");
        cliente.setNombre("Cliente de Prueba");
        cliente.setEmail("cliente@cinerama.pe");
        cliente.setTelefono("999888777");
        cliente.setContrasena(passwordEncoder.encode("123456"));
        cliente.setRol("CLIENTE");
        cliente.setActivo(1);
        cliente.setVerificado(1);
        clienteRepository.save(cliente);
        log.info("Datos demo: cuenta de cliente creada");
    }

    private List<Sala> crearSalas() {
        if (salaRepository.count() > 0) {
            return salaRepository.findAll();
        }

        List<Sala> salas = new ArrayList<>();
        salas.add(crearSala("Sala 1 - 2D", "2D", 6, 10));
        salas.add(crearSala("Sala 2 - 3D", "3D", 6, 8));
        salas.add(crearSala("Sala 3 - VIP", "VIP", 4, 6));
        log.info("Datos demo: {} salas con butacas creadas", salas.size());
        return salas;
    }

    private Sala crearSala(String nombre, String tipo, int filas, int butacasPorFila) {
        Sala sala = new Sala();
        sala.setNombre(nombre);
        sala.setTipo(tipo);
        sala.setCapacidadTotal(filas * butacasPorFila);
        sala.setActivo(1);
        sala = salaRepository.save(sala);

        List<Asiento> asientos = new ArrayList<>();
        for (int f = 0; f < filas; f++) {
            for (int numero = 1; numero <= butacasPorFila; numero++) {
                Asiento asiento = new Asiento();
                asiento.setSala(sala);
                asiento.setFila(FILAS[f]);
                asiento.setNumero(numero);
                asiento.setEstado("Disponible");
                asientos.add(asiento);
            }
        }
        asientoRepository.saveAll(asientos);
        return sala;
    }

    private void crearProductos() {
        if (productoRepository.count() > 0) {
            return;
        }
        productoRepository.saveAll(List.of(
                producto("Combo Familiar", "2 canchas grandes + 2 gaseosas", "35.90", "Combos", 18),
                producto("Combo Personal", "1 cancha mediana + 1 gaseosa", "19.90", "Combos", 30),
                producto("Cancha Grande", "Cancha grande de maíz", "8.50", "Snacks", 40),
                producto("Gaseosa Grande", "Bebida de 1 litro", "6.00", "Bebidas", 25),
                producto("Chocolate", "Barra de chocolate", "5.00", "Snacks", 12),
                producto("Agua Mineral", "Botella de 600 ml", "4.00", "Bebidas", 0)
        ));
        log.info("Datos demo: dulcería creada");
    }

    private Producto producto(String nombre, String descripcion, String precio, String categoria, int stock) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecio(new BigDecimal(precio));
        producto.setCategoria(categoria);
        producto.setActivo(1);
        producto.setStock(stock);
        return producto;
    }

    private void crearPromociones() {
        if (promocionRepository.count() > 0) {
            return;
        }
        promocionRepository.saveAll(List.of(
                promocion("CINERAMA10", "10% de descuento en tus butacas", 10),
                promocion("ESTUDIANTE15", "15% de descuento con carné de estudiante", 15)
        ));
        log.info("Datos demo: promociones creadas");
    }

    private Promocion promocion(String codigo, String descripcion, int descuento) {
        Promocion promocion = new Promocion();
        promocion.setCodigo(codigo);
        promocion.setDescripcion(descripcion);
        promocion.setDescuento(descuento);
        promocion.setActivo(1);
        return promocion;
    }

    private void crearFunciones(List<Sala> salas) {
        if (funcionRepository.count() > 0 || salas.isEmpty()) {
            return;
        }

        List<Pelicula> peliculas = peliculaRepository.findByActivo(1);
        if (peliculas.isEmpty()) {
            log.info("Datos demo: no hay películas activas, no se crean funciones");
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<Funcion> funciones = new ArrayList<>();
        int indiceSala = 0;

        for (Pelicula pelicula : peliculas.stream().limit(3).toList()) {
            for (int dia = 0; dia < 7; dia++) {
                LocalDate fecha = LocalDate.now().plusDays(dia);
                for (LocalTime hora : HORARIOS) {
                    LocalDateTime inicio = LocalDateTime.of(fecha, hora);
                    if (inicio.isBefore(ahora.plusMinutes(30))) {
                        continue;
                    }
                    Sala sala = salas.get(indiceSala % salas.size());
                    indiceSala++;

                    Funcion funcion = new Funcion();
                    funcion.setPelicula(pelicula);
                    funcion.setSala(sala);
                    funcion.setHoraInicio(inicio);
                    funcion.setEstado("PROGRAMADA");
                    funcion.setActivo(1);
                    funcion.setPrecio(duracionDe(pelicula) > 120
                            ? new BigDecimal("25.00")
                            : new BigDecimal("20.00"));
                    funciones.add(funcion);
                }
            }
        }

        funcionRepository.saveAll(funciones);
        log.info("Datos demo: {} funciones programadas", funciones.size());
    }

    private void completarPreciosDeFunciones() {
        List<Funcion> sinPrecio = funcionRepository.findByPrecioIsNull();
        if (sinPrecio.isEmpty()) {
            return;
        }
        int total = 0;
        for (Funcion funcion : sinPrecio) {
            int duracion = funcion.getPelicula() != null
                    && funcion.getPelicula().getDuracionMinutos() != null
                    ? funcion.getPelicula().getDuracionMinutos()
                    : 0;
            funcion.setPrecio(duracion > 120 ? new BigDecimal("25.00") : new BigDecimal("20.00"));
            total++;
        }
        funcionRepository.saveAll(sinPrecio);
        log.info("Datos demo: precio asignado a {} funciones antiguas", total);
    }

    private int duracionDe(Pelicula pelicula) {
        Integer duracion = pelicula != null ? pelicula.getDuracionMinutos() : null;
        return duracion != null ? duracion : 0;
    }
}
