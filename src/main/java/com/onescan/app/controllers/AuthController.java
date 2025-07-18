package com.onescan.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onescan.app.DTO.AuthenticationRequest;
import com.onescan.app.DTO.AuthenticationResponse;
import com.onescan.app.DTO.RegisterRequest;
import com.onescan.app.DTO.UserDTO;
import com.onescan.app.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok().build();
    }

    // Route mise à jour pour retourner un UserDTO
    @GetMapping("/user/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    // Nouvelle route pour mettre à jour un utilisateur
    @PutMapping("/update")
    public ResponseEntity<UserDTO> updateUser(
            @RequestBody UserDTO userDTO,
            HttpServletRequest request) {
        // Récupération de l'email depuis le token JWT (à implémenter selon votre
        // logique)
        String email = extractEmailFromToken(request);
        return ResponseEntity.ok(authService.updateUser(email, userDTO));
    }

    // Nouvelle route pour supprimer un utilisateur
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(HttpServletRequest request) {
        // Récupération de l'email depuis le token JWT (à implémenter selon votre
        // logique)
        String email = extractEmailFromToken(request);
        authService.deleteUser(email);
        return ResponseEntity.noContent().build();
    }

    // Méthode utilitaire pour extraire l'email du token JWT
    // À implémenter selon votre logique de gestion des tokens
    private String extractEmailFromToken(HttpServletRequest request) {
        // Cette méthode doit extraire l'email du token JWT
        // Voici un exemple de base (à adapter selon votre implémentation)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Utiliser votre JwtService pour extraire l'email du token
            // return jwtService.extractUsername(token);
        }
        throw new RuntimeException("Token not found or invalid");
    }
}