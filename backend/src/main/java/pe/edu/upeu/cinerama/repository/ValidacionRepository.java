package pe.edu.upeu.cinerama.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.cinerama.entity.Validacion;

import java.util.List;

@Repository
public interface ValidacionRepository extends JpaRepository<Validacion, Long> {

    @Query("""
            SELECT DISTINCT v FROM Validacion v
            LEFT JOIN FETCH v.compra c
            LEFT JOIN FETCH c.cliente
            LEFT JOIN FETCH c.funcion f
            LEFT JOIN FETCH f.pelicula
            LEFT JOIN FETCH f.sala
            LEFT JOIN FETCH c.detalles d
            LEFT JOIN FETCH d.asiento
            ORDER BY v.fecha DESC, v.idValidacion DESC
            """)
    List<Validacion> findRecientes();
}
