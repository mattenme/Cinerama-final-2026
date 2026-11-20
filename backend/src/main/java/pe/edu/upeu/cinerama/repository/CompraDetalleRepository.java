package pe.edu.upeu.cinerama.repository;

import pe.edu.upeu.cinerama.entity.CompraDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraDetalleRepository extends JpaRepository<CompraDetalle, Long> {

    boolean existsByFuncion_IdFuncionAndAsiento_IdAsientoAndCompra_EstadoIn(
            Long idFuncion, Long idAsiento, List<String> estados);

    @Query("""
            SELECT DISTINCT d.asiento.idAsiento FROM CompraDetalle d
            WHERE d.funcion.idFuncion = :idFuncion
              AND UPPER(d.compra.estado) IN ('COMPLETADA', 'CONFIRMADA', 'PENDIENTE')
            """)
    List<Long> findAsientosOcupados(@Param("idFuncion") Long idFuncion);
}
