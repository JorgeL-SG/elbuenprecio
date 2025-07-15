package com.tiendaabarrotes.elbuenprecio.Controller;

import com.tiendaabarrotes.elbuenprecio.Repository.UsuarioRepository;
import com.tiendaabarrotes.elbuenprecio.Model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        System.out.println("DEBUG: Accediendo a la página de login.");
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        System.out.println("DEBUG: Mostrando formulario de registro.");
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@Valid @ModelAttribute("usuario") Usuario usuario, BindingResult result, Model model) {
        System.out.println("DEBUG: Entrando al método registrarUsuario (POST).");
        System.out.println("DEBUG: Datos recibidos del formulario - Username: " + usuario.getUsername() + ", Email: " + usuario.getEmail());

        if (result.hasErrors()) {
            System.out.println("DEBUG: Errores de validación de campos detectados: " + result.getAllErrors());
            return "registro";
        }


        Usuario existingUsername = usuarioRepository.findByUsername(usuario.getUsername());
        if (existingUsername != null) {
            System.out.println("DEBUG: Username '" + usuario.getUsername() + "' ya está en uso.");
            model.addAttribute("error", "El nombre de usuario ya está en uso.");
            return "registro";
        }


        Usuario existingEmail = usuarioRepository.findByEmail(usuario.getEmail());
        if (existingEmail != null) {
            System.out.println("DEBUG: Email '" + usuario.getEmail() + "' ya está en uso.");
            model.addAttribute("error", "El email ya está en uso.");
            return "registro";
        }

        System.out.println("DEBUG: Username y Email no duplicados. Codificando contraseña.");
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol("CLIENTE");

        try {
            System.out.println("DEBUG: Intentando guardar usuario en la base de datos.");
            usuarioRepository.save(usuario);
            System.out.println("DEBUG: Usuario guardado exitosamente. Preparando redirección.");
            return "redirect:/login?registroExitoso";
        } catch (Exception e) {
            System.err.println("ERROR: Excepción capturada durante el guardado del usuario.");
            e.printStackTrace();
            model.addAttribute("error", "Ocurrió un error inesperado al registrar el usuario.");
            return "registro";
        }
    }
}