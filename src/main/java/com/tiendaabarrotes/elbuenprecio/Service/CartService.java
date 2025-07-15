package com.tiendaabarrotes.elbuenprecio.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import com.tiendaabarrotes.elbuenprecio.Model.*;
import com.tiendaabarrotes.elbuenprecio.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Transactional
    public Pedido placeOrder(Usuario usuario) {
        Carrito carrito = carritoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene un carrito activo."));

        if (carrito.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío, no se puede realizar un pedido.");
        }

        Pedido nuevoPedido = new Pedido();
        nuevoPedido.setUsuario(usuario);
        nuevoPedido.setFechaPedido(LocalDateTime.now());
        nuevoPedido.setTotal(carrito.getTotal());
        nuevoPedido.setEstado("PENDIENTE");

        nuevoPedido = pedidoRepository.save(nuevoPedido);

        List<DetallePedido> detallesPedido = new ArrayList<>();
        for (ItemCarrito itemCarrito : carrito.getItems()) {
            Producto producto = itemCarrito.getProducto();
            if (producto.getStock() < itemCarrito.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente para el producto: " + producto.getNombre());
            }

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(nuevoPedido);
            detalle.setProducto(producto);
            detalle.setCantidad(itemCarrito.getCantidad());
            detalle.setPrecioUnitario(itemCarrito.getPrecioUnitario());

            detallesPedido.add(detalle);

            producto.setStock(producto.getStock() - itemCarrito.getCantidad());
            productoRepository.save(producto);
        }

        detallePedidoRepository.saveAll(detallesPedido);
        nuevoPedido.setDetalles(detallesPedido);

        clearCart(carrito);

        return nuevoPedido;
    }

    @Transactional
    public Carrito getOrCreateCart(Usuario usuario) {
        Optional<Carrito> existingCart = carritoRepository.findByUsuario(usuario);
        if (existingCart.isPresent()) {
            return existingCart.get();
        } else {
            Carrito newCart = new Carrito(usuario);
            newCart.setTotal(BigDecimal.ZERO);
            return carritoRepository.save(newCart);
        }
    }

    @Transactional
    public Carrito addProductToCart(Carrito carrito, Long productoId, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));

        Optional<ItemCarrito> existingItem = itemCarritoRepository.findByCarritoAndProducto(carrito, producto);

        if (existingItem.isPresent()) {
            ItemCarrito item = existingItem.get();
            item.setCantidad(item.getCantidad() + cantidad);
            itemCarritoRepository.save(item);
        } else {
            ItemCarrito newItem = new ItemCarrito(carrito, producto, cantidad, producto.getPrecio());
            itemCarritoRepository.save(newItem);
            carrito.getItems().add(newItem);
        }

        carrito.calcularTotal();
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito updateProductQuantity(Carrito carrito, Long productoId, int newCantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));

        Optional<ItemCarrito> existingItem = itemCarritoRepository.findByCarritoAndProducto(carrito, producto);

        if (existingItem.isPresent()) {
            ItemCarrito item = existingItem.get();
            if (newCantidad <= 0) {
                itemCarritoRepository.delete(item);
                carrito.getItems().remove(item);
            } else {
                item.setCantidad(newCantidad);
                itemCarritoRepository.save(item);
            }
        } else {
        }

        carrito.calcularTotal();
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito removeProductFromCart(Carrito carrito, Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));

        Optional<ItemCarrito> existingItem = itemCarritoRepository.findByCarritoAndProducto(carrito, producto);

        if (existingItem.isPresent()) {
            ItemCarrito item = existingItem.get();
            itemCarritoRepository.delete(item);
            carrito.getItems().remove(item);
        }

        carrito.calcularTotal();
        return carritoRepository.save(carrito);
    }

    @Transactional
    public Carrito clearCart(Carrito carrito) {
        itemCarritoRepository.deleteAll(carrito.getItems());
        carrito.getItems().clear();
        carrito.setTotal(BigDecimal.ZERO);
        return carritoRepository.save(carrito);
    }
}