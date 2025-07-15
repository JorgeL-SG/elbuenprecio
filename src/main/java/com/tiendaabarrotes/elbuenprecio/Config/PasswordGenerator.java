package com.tiendaabarrotes.elbuenprecio.Config;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin1";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Contraseña cifrada para '" + rawPassword + "': " + encodedPassword);
    }
}