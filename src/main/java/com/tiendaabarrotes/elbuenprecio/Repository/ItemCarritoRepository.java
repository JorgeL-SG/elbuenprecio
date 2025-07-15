package com.tiendaabarrotes.elbuenprecio.Repository;

import com.tiendaabarrotes.elbuenprecio.Model.ItemCarrito;
import com.tiendaabarrotes.elbuenprecio.Model.Carrito;
import com.tiendaabarrotes.elbuenprecio.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    Optional<ItemCarrito> findByCarritoAndProducto(Carrito carrito, Producto producto);
}