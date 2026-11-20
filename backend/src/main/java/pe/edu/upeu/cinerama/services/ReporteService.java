package pe.edu.upeu.cinerama.services;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import pe.edu.upeu.cinerama.dto.ClienteVsPromedioDTO;
import pe.edu.upeu.cinerama.dto.ComparacionPeriodoDTO;
import pe.edu.upeu.cinerama.dto.OcupacionFuncionDTO;
import pe.edu.upeu.cinerama.dto.ResumenFuncionDTO;
import pe.edu.upeu.cinerama.dto.TopClienteDTO;
import pe.edu.upeu.cinerama.dto.VentaPeliculaDTO;
import pe.edu.upeu.cinerama.exception.ResourceNotFoundException;
import pe.edu.upeu.cinerama.repository.ClienteRepository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private static final int LIMITE_CLIENTES_POR_DEFECTO = 5;
    private static final int LIMITE_CLIENTES_MAXIMO = 50;
    private static final int DIAS_POR_DEFECTO = 30;
    private static final int DIAS_MAXIMO = 3650;

    private final ClienteRepository clienteRepository;
    private final SimpleJdbcCall resumenFuncionCall;
    private final SimpleJdbcCall ventasPeliculaCall;
    private final SimpleJdbcCall ocupacionCall;
    private final SimpleJdbcCall topClientesCall;
    private final SimpleJdbcCall comparacionCall;
    private final SimpleJdbcCall clienteVsPromedioCall;

    public ReporteService(JdbcTemplate jdbcTemplate, ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;

        this.resumenFuncionCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_RESUMEN_FUNCION")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_FUNCION", Types.NUMERIC),
                        new SqlOutParameter("P_BUTACAS", Types.NUMERIC),
                        new SqlOutParameter("P_INGRESOS_ENTRADAS", Types.NUMERIC),
                        new SqlOutParameter("P_INGRESOS_DULCERIA", Types.NUMERIC),
                        new SqlOutParameter("P_TOTAL", Types.NUMERIC)
                );

        this.ventasPeliculaCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_VENTAS_POR_PELICULA")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter("P_CURSOR", Types.REF_CURSOR, mapVentaPelicula())
                );

        this.ocupacionCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_OCUPACION_POR_FUNCION")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter("P_CURSOR", Types.REF_CURSOR, mapOcupacion())
                );

        this.topClientesCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_TOP_CLIENTES")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_LIMITE", Types.NUMERIC),
                        new SqlOutParameter("P_CURSOR", Types.REF_CURSOR, mapTopCliente())
                );

        this.comparacionCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_COMPARACION_PERIODOS")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_DIAS", Types.NUMERIC),
                        new SqlOutParameter("P_CURSOR", Types.REF_CURSOR, mapComparacion())
                );

        this.clienteVsPromedioCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SP_CLIENTE_VS_PROMEDIO")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_ID_CLIENTE", Types.NUMERIC),
                        new SqlOutParameter("P_DNI", Types.VARCHAR),
                        new SqlOutParameter("P_NOMBRE", Types.VARCHAR),
                        new SqlOutParameter("P_COMPRAS", Types.NUMERIC),
                        new SqlOutParameter("P_TOTAL_CLIENTE", Types.NUMERIC),
                        new SqlOutParameter("P_PROMEDIO", Types.NUMERIC),
                        new SqlOutParameter("P_DIFERENCIA", Types.NUMERIC),
                        new SqlOutParameter("P_PORCENTAJE", Types.NUMERIC),
                        new SqlOutParameter("P_POSICION", Types.NUMERIC),
                        new SqlOutParameter("P_CLIENTES", Types.NUMERIC)
                );
    }

    public ResumenFuncionDTO resumenFuncion(Long idFuncion) {
        Map<String, Object> salida = resumenFuncionCall.execute(Map.of("P_ID_FUNCION", idFuncion));

        return new ResumenFuncionDTO(
                idFuncion,
                entero(salida.get("P_BUTACAS")),
                decimal(salida.get("P_INGRESOS_ENTRADAS")),
                decimal(salida.get("P_INGRESOS_DULCERIA")),
                decimal(salida.get("P_TOTAL"))
        );
    }

    public List<VentaPeliculaDTO> ventasPorPelicula() {
        return filas(ventasPeliculaCall.execute(), "P_CURSOR");
    }

    public List<OcupacionFuncionDTO> ocupacionPorFuncion() {
        return filas(ocupacionCall.execute(), "P_CURSOR");
    }

    public List<TopClienteDTO> topClientes(Integer limite) {
        int valor = limite == null ? LIMITE_CLIENTES_POR_DEFECTO : limite;
        valor = Math.max(1, Math.min(LIMITE_CLIENTES_MAXIMO, valor));

        return filas(topClientesCall.execute(Map.of("P_LIMITE", valor)), "P_CURSOR");
    }

    public List<ComparacionPeriodoDTO> comparacionPeriodos(Integer dias) {
        int valor = dias == null ? DIAS_POR_DEFECTO : dias;
        valor = Math.max(1, Math.min(DIAS_MAXIMO, valor));

        return filas(comparacionCall.execute(Map.of("P_DIAS", valor)), "P_CURSOR");
    }

    public ClienteVsPromedioDTO clienteVsPromedio(Long idCliente) {
        if (idCliente == null || !clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException("Cliente no encontrado");
        }

        Map<String, Object> salida;
        try {
            salida = clienteVsPromedioCall.execute(Map.of("P_ID_CLIENTE", idCliente));
        } catch (DataAccessException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("CLIENTE_NO_EXISTE")) {
                throw new ResourceNotFoundException("Cliente no encontrado");
            }
            throw ex;
        }

        return new ClienteVsPromedioDTO(
                idCliente,
                texto(salida.get("P_DNI")),
                texto(salida.get("P_NOMBRE")),
                entero(salida.get("P_COMPRAS")),
                decimal(salida.get("P_TOTAL_CLIENTE")),
                decimal(salida.get("P_PROMEDIO")),
                decimal(salida.get("P_DIFERENCIA")),
                decimal(salida.get("P_PORCENTAJE")),
                salida.get("P_POSICION") == null ? null : entero(salida.get("P_POSICION")),
                entero(salida.get("P_CLIENTES"))
        );
    }

    private RowMapper<VentaPeliculaDTO> mapVentaPelicula() {
        return (rs, fila) -> new VentaPeliculaDTO(
                rs.getLong("ID_PELICULA"),
                rs.getString("TITULO"),
                rs.getInt("COMPRAS"),
                rs.getInt("BUTACAS"),
                decimal(rs.getBigDecimal("ENTRADAS")),
                decimal(rs.getBigDecimal("DULCERIA")),
                decimal(rs.getBigDecimal("TOTAL"))
        );
    }

    private RowMapper<OcupacionFuncionDTO> mapOcupacion() {
        return (rs, fila) -> {
            var hora = rs.getTimestamp("HORA_INICIO");
            return new OcupacionFuncionDTO(
                    rs.getLong("ID_FUNCION"),
                    rs.getString("TITULO"),
                    rs.getString("SALA"),
                    hora == null ? null : hora.toLocalDateTime(),
                    rs.getInt("CAPACIDAD"),
                    rs.getInt("COMPRAS"),
                    rs.getInt("VENDIDAS"),
                    decimal(rs.getBigDecimal("OCUPACION_PCT"))
            );
        };
    }

    private RowMapper<TopClienteDTO> mapTopCliente() {
        return (rs, fila) -> {
            var ultima = rs.getTimestamp("ULTIMA_COMPRA");
            return new TopClienteDTO(
                    rs.getInt("POSICION"),
                    rs.getString("DNI"),
                    rs.getString("NOMBRE"),
                    rs.getInt("COMPRAS"),
                    rs.getInt("BUTACAS"),
                    decimal(rs.getBigDecimal("ENTRADAS")),
                    decimal(rs.getBigDecimal("DULCERIA")),
                    decimal(rs.getBigDecimal("TOTAL")),
                    decimal(rs.getBigDecimal("TICKET_PROMEDIO")),
                    decimal(rs.getBigDecimal("DIFERENCIA_PROMEDIO")),
                    decimal(rs.getBigDecimal("VS_PROMEDIO_PCT")),
                    ultima == null ? null : ultima.toLocalDateTime()
            );
        };
    }

    private RowMapper<ComparacionPeriodoDTO> mapComparacion() {
        return (rs, fila) -> new ComparacionPeriodoDTO(
                rs.getString("METRICA"),
                decimal(rs.getBigDecimal("ACTUAL")),
                decimal(rs.getBigDecimal("ANTERIOR")),
                decimal(rs.getBigDecimal("VARIACION")),
                rs.getBigDecimal("VARIACION_PCT")
        );
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> filas(Map<String, Object> salida, String parametro) {
        Object valor = salida.get(parametro);
        return valor instanceof List<?> lista ? (List<T>) lista : List.of();
    }

    private int entero(Object valor) {
        return valor instanceof Number numero ? numero.intValue() : 0;
    }

    private BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        return valor instanceof Number numero
                ? BigDecimal.valueOf(numero.doubleValue())
                : new BigDecimal(valor.toString());
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }
}
