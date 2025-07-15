package com.tiendaabarrotes.elbuenprecio.Controller;

import com.tiendaabarrotes.elbuenprecio.Model.Pedido;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import com.tiendaabarrotes.elbuenprecio.Repository.PedidoRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.CategoriaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.MarcaRepository;
import com.tiendaabarrotes.elbuenprecio.Service.CartService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

import static com.tiendaabarrotes.elbuenprecio.Controller.CartController.SESSION_CART_KEY;

@Controller
@RequestMapping("/pedidos")
public class ClientOrderController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private CartService cartService;

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
            return (sessionCartMap != null) ? sessionCartMap.values().stream().mapToInt(Integer::intValue).sum() : 0;
        }
    }

    @GetMapping("/mis-compras")
    @PreAuthorize("isAuthenticated()")
    public String showMyOrders(Model model, HttpSession session) {
        Usuario currentUser = getAuthenticatedUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Pedido> misCompras = pedidoRepository.findByUsuario(currentUser);
        model.addAttribute("misCompras", misCompras);

        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAll());
        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
        model.addAttribute("timestamp", System.currentTimeMillis());
        return "mis-compras";
    }
}