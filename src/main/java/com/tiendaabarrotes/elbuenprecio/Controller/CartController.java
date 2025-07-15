package com.tiendaabarrotes.elbuenprecio.Controller;

import com.tiendaabarrotes.elbuenprecio.Model.Carrito;
import com.tiendaabarrotes.elbuenprecio.Model.ItemCarrito;
import com.tiendaabarrotes.elbuenprecio.Model.Pedido;
import com.tiendaabarrotes.elbuenprecio.Model.Producto;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import com.tiendaabarrotes.elbuenprecio.Repository.ProductoRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.MarcaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.CategoriaRepository;
import com.tiendaabarrotes.elbuenprecio.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Controller
@RequestMapping("/carrito")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;


    public static final String SESSION_CART_KEY = "sessionCart";

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

    @GetMapping
    public String viewCart(Model model, HttpSession session) {
        Usuario currentUser = getAuthenticatedUser();
        List<ItemCarrito> itemsToDisplay = new ArrayList<>();
        BigDecimal cartTotal = BigDecimal.ZERO;

        if (currentUser != null) {
            Carrito persistentCart = cartService.getOrCreateCart(currentUser);
            itemsToDisplay.addAll(persistentCart.getItems());
            cartTotal = persistentCart.getTotal();
        } else {
            Map<Long, Integer> sessionCartMap = (Map<Long, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (sessionCartMap != null && !sessionCartMap.isEmpty()) {
                for (Map.Entry<Long, Integer> entry : sessionCartMap.entrySet()) {
                    Long productId = entry.getKey();
                    Integer quantity = entry.getValue();
                    Optional<Producto> productOpt = productoRepository.findById(productId);
                    if (productOpt.isPresent()) {
                        Producto product = productOpt.get();
                        BigDecimal itemSubtotal = product.getPrecio().multiply(BigDecimal.valueOf(quantity));

                        ItemCarrito tempItem = new ItemCarrito();
                        tempItem.setProducto(product);
                        tempItem.setCantidad(quantity);
                        tempItem.setPrecioUnitario(product.getPrecio());
                        tempItem.setId(null);

                        itemsToDisplay.add(tempItem);
                        cartTotal = cartTotal.add(itemSubtotal);
                    }
                }
            }
        }

        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAllByOrderByOrdenAscNombreAsc());

        model.addAttribute("cartItems", itemsToDisplay);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
        return "carrito";
    }

    @PostMapping("/add")
    public String addProductToCart(@RequestParam("productoId") Long productoId,
                                   @RequestParam(value = "cantidad", defaultValue = "1") int cantidad,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        if (cantidad <= 0) {
            redirectAttributes.addFlashAttribute("error", "La cantidad debe ser mayor que cero.");
            return "redirect:/ofertas";
        }

        Usuario currentUser = getAuthenticatedUser();
        if (currentUser != null) {
            try {
                cartService.addProductToCart(cartService.getOrCreateCart(currentUser), productoId, cantidad);
                redirectAttributes.addFlashAttribute("success", "Producto añadido al carrito.");
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        } else {
            Map<Long, Integer> sessionCartMap = (Map<Long, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (sessionCartMap == null) {
                sessionCartMap = new HashMap<>();
            }

            Optional<Producto> productOpt = productoRepository.findById(productoId);
            if (!productOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Producto no encontrado.");
                return "redirect:/ofertas";
            }
            Producto producto = productOpt.get();

            sessionCartMap.merge(productoId, cantidad, Integer::sum);

            session.setAttribute(SESSION_CART_KEY, sessionCartMap);
            redirectAttributes.addFlashAttribute("success", "Producto añadido al carrito de sesión.");
        }
        return "redirect:/carrito";
    }

    @PostMapping("/update")
    public String updateCartItem(@RequestParam("productoId") Long productoId,
                                 @RequestParam("cantidad") int cantidad,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        Usuario currentUser = getAuthenticatedUser();
        if (currentUser != null) {
            try {
                cartService.updateProductQuantity(cartService.getOrCreateCart(currentUser), productoId, cantidad);
                redirectAttributes.addFlashAttribute("success", "Cantidad actualizada.");
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        } else {
            Map<Long, Integer> sessionCartMap = (Map<Long, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (sessionCartMap != null) {
                if (cantidad <= 0) {
                    sessionCartMap.remove(productoId);
                } else {
                    if (productoRepository.existsById(productoId)) {
                        sessionCartMap.put(productoId, cantidad);
                    } else {
                        redirectAttributes.addFlashAttribute("error", "Producto no encontrado en el sistema.");
                        return "redirect:/carrito";
                    }
                }
                session.setAttribute(SESSION_CART_KEY, sessionCartMap);
                redirectAttributes.addFlashAttribute("success", "Cantidad de carrito de sesión actualizada.");
            }
        }
        return "redirect:/carrito";
    }

    @PostMapping("/remove")
    public String removeProductFromCart(@RequestParam("productoId") Long productoId,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {

        Usuario currentUser = getAuthenticatedUser();
        if (currentUser != null) {
            try {
                cartService.removeProductFromCart(cartService.getOrCreateCart(currentUser), productoId);
                redirectAttributes.addFlashAttribute("success", "Producto eliminado del carrito.");
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        } else {
            Map<Long, Integer> sessionCartMap = (Map<Long, Integer>) session.getAttribute(SESSION_CART_KEY);
            if (sessionCartMap != null) {
                sessionCartMap.remove(productoId);
                session.setAttribute(SESSION_CART_KEY, sessionCartMap);
                redirectAttributes.addFlashAttribute("success", "Producto eliminado del carrito de sesión.");
            }
        }
        return "redirect:/carrito";
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public String checkout(HttpSession session, RedirectAttributes redirectAttributes) {

        Usuario currentUser = getAuthenticatedUser();
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión para finalizar la compra.");
            return "redirect:/login";
        }

        try {
            Pedido pedidoConfirmado = cartService.placeOrder(currentUser);
            redirectAttributes.addFlashAttribute("success", "¡Tu compra ha sido realizada con éxito! Pedido #" + pedidoConfirmado.getId());
            return "redirect:/confirmacion-compra";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado al procesar tu compra.");
            return "redirect:/carrito";
        }
    }
}