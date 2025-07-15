package com.tiendaabarrotes.elbuenprecio.Service;

import com.tiendaabarrotes.elbuenprecio.Model.Carrito;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;


@Component
public class CartMergeListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final CartService cartService;
    private final UsuarioRepository usuarioRepository;
    private final HttpSession httpSession;

    public CartMergeListener(CartService cartService, UsuarioRepository usuarioRepository, HttpSession httpSession) {
        this.cartService = cartService;
        this.usuarioRepository = usuarioRepository;
        this.httpSession = httpSession;
    }

    @Override
    @Transactional
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        UserDetails userDetails = (UserDetails) event.getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        Usuario authenticatedUser = usuarioRepository.findByUsername(username);

        Map<Long, Integer> sessionCartMap = (Map<Long, Integer>) httpSession.getAttribute("sessionCart");

        if (authenticatedUser != null && sessionCartMap != null && !sessionCartMap.isEmpty()) {
            Carrito persistentCart = cartService.getOrCreateCart(authenticatedUser);

            for (Map.Entry<Long, Integer> entry : sessionCartMap.entrySet()) {
                Long productId = entry.getKey();
                Integer quantity = entry.getValue();

                try {
                    cartService.addProductToCart(persistentCart, productId, quantity);
                } catch (IllegalArgumentException e) {
                    System.err.println("Advertencia: Producto con ID " + productId + " en carrito de sesión no encontrado en DB. No se pudo añadir a carrito persistente. Mensaje: " + e.getMessage());
                }
            }

            httpSession.removeAttribute("sessionCart");
            System.out.println("DEBUG: Carrito de sesión fusionado y limpiado para el usuario: " + username);
        }
    }
}