package com.tiendaabarrotes.elbuenprecio.Repository;

import com.tiendaabarrotes.elbuenprecio.Model.Pedido;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {
    List<Pedido> findByUsuario(Usuario usuario);
}