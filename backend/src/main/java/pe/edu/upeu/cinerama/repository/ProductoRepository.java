package pe.edu.upeu.cinerama.repository;

import pe.edu.upeu.cinerama.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivo(Integer activo);

    List<Producto> findByCategoriaAndActivo(String categoria, Integer activo);

    List<Producto> findByCategoriaOrderByNombreAsc(String categoria);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Producto p
               SET p.stock = p.stock - :cantidad
             WHERE p.idProducto = :idProducto
               AND p.stock IS NOT NULL
               AND p.stock >= :cantidad
            """)
    int descontarStock(@Param("idProducto") Long idProducto, @Param("cantidad") int cantidad);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Producto p
               SET p.stock = p.stock + :cantidad
             WHERE p.idProducto = :idProducto
               AND p.stock IS NOT NULL
            """)
    int reponerStock(@Param("idProducto") Long idProducto, @Param("cantidad") int cantidad);

    List<Producto> findByNombre(String nombre);
}