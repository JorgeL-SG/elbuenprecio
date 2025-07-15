package com.tiendaabarrotes.elbuenprecio.Controller;

import com.tiendaabarrotes.elbuenprecio.Model.Pedido;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import com.tiendaabarrotes.elbuenprecio.Repository.CategoriaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.MarcaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.PedidoRepository;
import com.tiendaabarrotes.elbuenprecio.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.tiendaabarrotes.elbuenprecio.Controller.CartController.SESSION_CART_KEY;

@Controller
@RequestMapping("/admin/pedidos")
public class AdminPedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository usuarioRepository;

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


    @GetMapping("/lista")
    public String listarPedidos(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "estado", required = false) String estado,
            Model model,
            HttpSession session) {

        Specification<Pedido> spec = Specification.where(null);

        if (query != null && !query.isEmpty()) {
            Specification<Pedido> querySpec = (root, q, cb) -> {
                String likePattern = "%" + query.toLowerCase() + "%";
                if (query.matches("\\d+")) {
                    return cb.or(
                            cb.like(cb.lower(root.get("usuario").get("username")), likePattern),
                            cb.equal(root.get("id"), Long.parseLong(query))
                    );
                }
                return cb.like(cb.lower(root.get("usuario").get("username")), likePattern);
            };
            spec = spec.and(querySpec);
        }

        if (estado != null && !estado.isEmpty()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("estado"), estado));
        }

        List<Pedido> pedidos = pedidoRepository.findAll(spec);
        model.addAttribute("pedidos", pedidos);

        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAll());
        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
        model.addAttribute("timestamp", System.currentTimeMillis());
        return "admin/pedido-lista";
    }

    @GetMapping("/detalles/{id}")
    public String verDetallesPedido(@PathVariable Long id, Model model, HttpSession session) {
        Optional<Pedido> pedidoOptional = pedidoRepository.findById(id);

        if (pedidoOptional.isPresent()) {
            Pedido pedido = pedidoOptional.get();
            model.addAttribute("pedido", pedido);

            model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
            model.addAttribute("marcasPrincipales", marcaRepository.findAll());
            model.addAttribute("cartItemCount", getCartItemCount(session));
            model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
            model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
            model.addAttribute("timestamp", System.currentTimeMillis());
            return "admin/pedido-detalles";
        } else {
            return "redirect:/admin/pedidos/lista";
        }
    }

    @PostMapping("/actualizar-estado")
    public String actualizarEstadoPedido(@RequestParam("pedidoId") Long pedidoId,
                                         @RequestParam("nuevoEstado") String nuevoEstado,
                                         RedirectAttributes redirectAttributes) {
        Optional<Pedido> pedidoOptional = pedidoRepository.findById(pedidoId);

        if (pedidoOptional.isPresent()) {
            Pedido pedido = pedidoOptional.get();
            pedido.setEstado(nuevoEstado);
            pedidoRepository.save(pedido);
            redirectAttributes.addFlashAttribute("successMessage", "Estado del pedido #" + pedidoId + " actualizado a " + nuevoEstado + ".");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Pedido no encontrado con ID: " + pedidoId + ".");
        }
        return "redirect:/admin/pedidos/detalles/" + pedidoId;
    }
}