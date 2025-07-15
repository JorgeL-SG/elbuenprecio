package com.tiendaabarrotes.elbuenprecio.Repository;

import com.tiendaabarrotes.elbuenprecio.Model.Categoria;
import com.tiendaabarrotes.elbuenprecio.Model.Marca;
import com.tiendaabarrotes.elbuenprecio.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoria(Categoria categoria);
    List<Producto> findByMarca(Marca marca);
    List<Producto> findByEnOfertaTrue();
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
}