package com.tiendaabarrotes.elbuenprecio.Controller;

import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import com.tiendaabarrotes.elbuenprecio.Repository.CategoriaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.MarcaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository;
import com.tiendaabarrotes.elbuenprecio.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;

import static com.tiendaabarrotes.elbuenprecio.Controller.CartController.SESSION_CART_KEY;

@Controller
public class ConfirmacionController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return null;
        }
        return usuarioRepository.findByUsername(authentication.getName());
    }

    private int getCartItemCount(HttpSession session) {
        Usuario currentUser = getAuthenticatedUser();
        if (currentUser != null) {
            return cartService.getOrCreateCart(currentUser).getItems().size();
        } else {
            Map<Long, Integer> sessionCartMap = (Map<Long, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (sessionCartMap != null) {
                int count = sessionCartMap.values().stream().mapToInt(Integer::intValue).sum();
                return count;
            }
            return 0;
        }
    }

    @GetMapping("/confirmacion-compra")
    public String showConfirmation(Model model, HttpSession session) {
        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAllByOrderByOrdenAscNombreAsc());

        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
        model.addAttribute("timestamp", System.currentTimeMillis());
        return "confirmacion-compra";
    }
}