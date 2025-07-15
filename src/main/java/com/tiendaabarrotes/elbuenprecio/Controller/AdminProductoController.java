package com.tiendaabarrotes.elbuenprecio.Controller;

import com.tiendaabarrotes.elbuenprecio.Model.Producto;
import com.tiendaabarrotes.elbuenprecio.Repository.CategoriaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.MarcaRepository;
import com.tiendaabarrotes.elbuenprecio.Repository.ProductoRepository;
import com.tiendaabarrotes.elbuenprecio.Service.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import static com.tiendaabarrotes.elbuenprecio.Controller.CartController.SESSION_CART_KEY;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import com.tiendaabarrotes.elbuenprecio.Service.CartService;
import jakarta.servlet.http.HttpSession;



@Controller
@RequestMapping("/admin")
public class AdminProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository usuarioRepository;
    @Value("${app.upload.dir}")
    private String uploadDir;

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


    @GetMapping({"", "/dashboard"})
    public String adminDashboard(Model model) {
        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAll());
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
        model.addAttribute("cartItemCount", 0);
        return "admin/dashboard";
    }


    @GetMapping("/productos/lista")
    public String listarProductos(Model model, HttpSession session,
                                  @RequestParam(value = "search", required = false) String searchTerm) {

        List<Producto> productos;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            productos = productoRepository.findByNombreContainingIgnoreCase(searchTerm.trim());
            model.addAttribute("searchTerm", searchTerm);
        } else {
            productos = productoRepository.findAll();
        }

        model.addAttribute("productos", productos);

        // Datos para el navbar
        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAllByOrderByOrdenAscNombreAsc());
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
        model.addAttribute("cartItemCount", getCartItemCount(session));

        model.addAttribute("timestamp", System.currentTimeMillis());

        return "admin/producto-lista";
    }

    @GetMapping("/productos/nuevo")
    public String mostrarFormularioNuevoProducto(Model model, HttpSession session) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("marcas", marcaRepository.findAll());
        model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
        model.addAttribute("marcasPrincipales", marcaRepository.findAll());
        model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
        model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
        model.addAttribute("cartItemCount", getCartItemCount(session));
        return "admin/producto-form";
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(@Valid @ModelAttribute("producto") Producto producto,
                                  BindingResult result,
                                  @RequestParam(value = "fileImage", required = false) MultipartFile file,
                                  @RequestParam(value = "existingImageName", required = false) String existingImageName,
                                  @RequestParam(value = "enOferta", required = false) String enOfertaCheckboxValue,
                                  Model model,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {

        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaRepository.findAll());
            model.addAttribute("marcas", marcaRepository.findAll());
            model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
            model.addAttribute("marcasPrincipales", marcaRepository.findAll());
            model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
            model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
            model.addAttribute("cartItemCount", getCartItemCount(session));
            return "admin/producto-form";
        }

        // ----- INICIO DE LA SECCIÓN MODIFICADA -----
        if (file != null && !file.isEmpty()) {
            try {
                // 1. Subimos el archivo a Cloudinary y obtenemos la URL segura
                String imageUrl = cloudinaryService.uploadFile(file);

                // 2. Guardamos esa URL completa en el producto
                producto.setNombreImagen(imageUrl);

            } catch (IOException e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Error al subir la imagen: " + e.getMessage());
                model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
                model.addAttribute("marcasPrincipales", marcaRepository.findAll());
                model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
                model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
                model.addAttribute("cartItemCount", getCartItemCount(session));
                return "admin/producto-form";
            }
        } else if (producto.getId() != null && existingImageName != null && !existingImageName.isEmpty()) {
            // Esta parte se queda igual, asume que existingImageName ya es la URL de una imagen existente
            producto.setNombreImagen(existingImageName);
        } else {
            producto.setNombreImagen(null);
        }
        // ----- FIN DE LA SECCIÓN MODIFICADA -----

        producto.setEnOferta("on".equals(enOfertaCheckboxValue));
        productoRepository.save(producto);
        redirectAttributes.addFlashAttribute("successMessage", "Producto guardado exitosamente.");
        return "redirect:/admin/productos/lista";
    }

    @GetMapping("/productos/editar/{id}")
    public String mostrarFormularioEditarProducto(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        Optional<Producto> productoOptional = productoRepository.findById(id);
        if (productoOptional.isPresent()) {
            model.addAttribute("producto", productoOptional.get());
            model.addAttribute("categorias", categoriaRepository.findAll());
            model.addAttribute("marcas", marcaRepository.findAll());
            model.addAttribute("categoriasPrincipales", categoriaRepository.findAll());
            model.addAttribute("marcasPrincipales", marcaRepository.findAll());
            model.addAttribute("isAuthenticated", getAuthenticatedUser() != null);
            model.addAttribute("currentUsername", (getAuthenticatedUser() != null) ? getAuthenticatedUser().getUsername() : null);
            model.addAttribute("cartItemCount", getCartItemCount(session));
            return "admin/producto-form";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Producto no encontrado.");
            return "redirect:/admin/productos/lista";
        }
    }

    @GetMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        Optional<Producto> productoOptional = productoRepository.findById(id);
        if (productoOptional.isPresent()) {
            Producto producto = productoOptional.get();
            if (producto.getNombreImagen() != null && !producto.getNombreImagen().isEmpty()) {
                try {
                    Path imagePath = Paths.get(uploadDir + producto.getNombreImagen());
                    Files.deleteIfExists(imagePath);
                    System.out.println("DEBUG: Imagen eliminada: " + imagePath.toString());
                } catch (IOException e) {
                    System.err.println("ERROR: No se pudo eliminar la imagen del producto: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        productoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Producto eliminado exitosamente.");
        return "redirect:/admin/productos/lista";
    }
}