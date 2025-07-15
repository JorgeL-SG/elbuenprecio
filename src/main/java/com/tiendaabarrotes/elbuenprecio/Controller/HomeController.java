package com.tiendaabarrotes.elbuenprecio.Controller;

import com.tiendaabarrotes.elbuenprecio.Model.Categoria;
import com.tiendaabarrotes.elbuenprecio.Model.Marca;
import com.tiendaabarrotes.elbuenprecio.Model.Producto;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import com.tiendaabarrotes.elbuenprecio.Repository.CategoriaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.MarcaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.ProductoRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository;
import com.tiendaabarrotes.elbuenprecio.Service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

import static com.tiendaabarrotes.elbuenprecio.Controller.CartController.SESSION_CART_KEY;

@Controller
public class HomeController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ProductoRepository productoRepository;

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
            return (sessionCartMap != null) ? sessionCartMap.values().stream().mapToInt(Integer::intValue).sum() : 0;
        }
    }


    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        List<Categoria> categorias = categoriaRepository.findAll();
        model.addAttribute("categoriasPrincipales", categorias);

        List<Marca> marcas = marcaRepository.findAllByOrderByOrdenAscNombreAsc();
        model.addAttribute("marcasPrincipales", marcas);

        model.addAttribute("tecnologiaId", categorias.stream().filter(c -> "Tecnología".equals(c.getNombre())).findFirst().map(Categoria::getId).orElse(0L));
        model.addAttribute("accesoriosId", categorias.stream().filter(c -> "Accesorios".equals(c.getNombre())).findFirst().map(Categoria::getId).orElse(0L));
        model.addAttribute("electrodomesticosId", categorias.stream().filter(c -> "Electrodomésticos".equals(c.getNombre())).findFirst().map(Categoria::getId).orElse(0L));
        model.addAttribute("hogarId", categorias.stream().filter(c -> "Hogar".equals(c.getNombre())).findFirst().map(Categoria::getId).orElse(0L));

        model.addAttribute("abarrotesId", categorias.stream().filter(c -> "Abarrotes".equals(c.getNombre())).findFirst().map(Categoria::getId).orElse(0L));

        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);

        return "index";
    }

    @GetMapping("/ofertas")
    public String verOfertas(@RequestParam(value = "categoria", required = false) Long categoriaId,
                             @RequestParam(value = "marca", required = false) Long marcaId,
                             Model model, HttpSession session) {

        List<Producto> productos;
        String titulo = "Nuestras Ofertas";
        boolean soloOfertasActivo = false;

        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
            if (categoria != null) {
                productos = productoRepository.findByCategoria(categoria);
                titulo = "Productos de " + categoria.getNombre();
            } else {
                productos = productoRepository.findAll();
                titulo = "Productos (Categoría no encontrada)";
            }
            soloOfertasActivo = false;
        } else if (marcaId != null) {
            Marca marca = marcaRepository.findById(marcaId).orElse(null);
            if (marca != null) {
                productos = productoRepository.findByMarca(marca);
                titulo = "Productos de " + marca.getNombre();
            } else {
                productos = productoRepository.findAll();
                titulo = "Productos (Marca no encontrada)";
            }
            soloOfertasActivo = false;
        } else {
            productos = productoRepository.findByEnOfertaTrue();
            titulo = "Ofertas Especiales";
            soloOfertasActivo = true;
        }

        model.addAttribute("productos", productos);
        model.addAttribute("tituloSeccion", titulo);
        model.addAttribute("soloOfertas", soloOfertasActivo);

        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAllByOrderByOrdenAscNombreAsc());

        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);

        return "ofertas";
    }

    @GetMapping("/contactanos")
    public String contactanos(Model model, HttpSession session) {
        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAllByOrderByOrdenAscNombreAsc());

        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);

        return "contactanos";
    }

    @GetMapping("/todos-los-productos")
    public String verTodosLosProductos(Model model, HttpSession session) {
        List<Producto> productos = productoRepository.findAll();
        model.addAttribute("productos", productos);
        model.addAttribute("tituloSeccion", "Todos los Productos de la Tienda");
        model.addAttribute("soloOfertas", false);

        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAllByOrderByOrdenAscNombreAsc());
        model.addAttribute("cartItemCount", getCartItemCount(session));
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);

        return "ofertas";
    }
}
