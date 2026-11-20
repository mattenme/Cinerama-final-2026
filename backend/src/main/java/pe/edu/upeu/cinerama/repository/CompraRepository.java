package pe.edu.upeu.cinerama.repository;

import jakarta.persistence.LockModeType;
import pe.edu.upeu.cinerama.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Query("""
            SELECT DISTINCT c FROM Compra c
            JOIN FETCH c.cliente
            JOIN FETCH c.funcion f
            JOIN FETCH f.pelicula
            JOIN FETCH f.sala
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.asiento
            WHERE c.cliente.idCliente = :idCliente
            ORDER BY c.fechaCompra DESC, c.idCompra DESC
            """)
    List<Compra> findByClienteConRelaciones(@Param("idCliente") Long idCliente);

    @Query("""
            SELECT DISTINCT c FROM Compra c
            JOIN FETCH c.cliente
            JOIN FETCH c.funcion f
            JOIN FETCH f.pelicula
            JOIN FETCH f.sala
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.asiento
            WHERE c.funcion.idFuncion = :idFuncion
            ORDER BY c.fechaCompra DESC, c.idCompra DESC
            """)
    List<Compra> findByFuncionConRelaciones(@Param("idFuncion") Long idFuncion);

    @Query("""
            SELECT DISTINCT c FROM Compra c
            JOIN FETCH c.cliente
            JOIN FETCH c.funcion f
            JOIN FETCH f.pelicula
            JOIN FETCH f.sala
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.asiento
            WHERE c.funcion.idFuncion = :idFuncion AND UPPER(c.estado) IN ('COMPLETADA', 'CONFIRMADA')
            ORDER BY c.fechaCompra DESC, c.idCompra DESC
            """)
    List<Compra> findConfirmedByFuncion(@Param("idFuncion") Long idFuncion);

    @Query("""
            SELECT DISTINCT c FROM Compra c
            JOIN FETCH c.cliente
            JOIN FETCH c.funcion f
            JOIN FETCH f.pelicula
            JOIN FETCH f.sala
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.asiento
            ORDER BY c.fechaCompra DESC, c.idCompra DESC
            """)
    List<Compra> findAllConRelaciones();

    @Query("""
            SELECT c FROM Compra c
            JOIN FETCH c.cliente
            JOIN FETCH c.funcion f
            JOIN FETCH f.pelicula
            JOIN FETCH f.sala
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.asiento
            WHERE c.idCompra = :id
            """)
    Optional<Compra> findByIdConRelaciones(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT c FROM Compra c
            JOIN FETCH c.funcion f
            JOIN FETCH f.pelicula p
            WHERE c.cliente.idCliente = :idCliente AND p.idPelicula = :idPelicula
            """)
    List<Compra> findByClienteYPelicula(@Param("idCliente") Long idCliente,
                                        @Param("idPelicula") Long idPelicula);

    @Query("SELECT c.cliente.idCliente FROM Compra c WHERE c.idCompra = :idCompra")
    Optional<Long> buscarIdClientePorCompra(@Param("idCompra") Long idCompra);

    @Query("""
            SELECT c FROM Compra c
            JOIN FETCH c.funcion
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.asiento
            WHERE UPPER(c.estado) = 'PENDIENTE'
              AND c.fechaCompra < :limite
            """)
    List<Compra> findPendientesAnterioresA(@Param("limite") Timestamp limite);

    Optional<Compra> findFirstByCodigoQrOrderByIdCompraAsc(String codigoQr);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Compra c WHERE c.codigoQr = :codigo ORDER BY c.idCompra ASC")
    List<Compra> buscarPorCodigoQrParaValidar(@Param("codigo") String codigo);
}
