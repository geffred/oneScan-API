package com.onescan.app.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.onescan.app.DTO.AuthenticationRequest;
import com.onescan.app.DTO.AuthenticationResponse;
import com.onescan.app.DTO.RegisterRequest;
import com.onescan.app.DTO.UserDTO;
import com.onescan.app.Entity.Role;
import com.onescan.app.Entity.User;
import com.onescan.app.Security.JwtService;
import com.onescan.app.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .country(request.country())
                .companyType(request.companyType())
                .newsletter(request.newsletter())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }

    public AuthenticationResponse login(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()));

        var user = userRepository.findByEmail(request.email())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
    }

    // Méthode mise à jour pour retourner un UserDTO
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDTO.fromEntity(user);
    }

    // Méthode pour mettre à jour un utilisateur
    public UserDTO updateUser(String email, UserDTO userDTO) {
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Mise à jour des champs (sans toucher au mot de passe et à l'email)
        existingUser.setFirstName(userDTO.firstName());
        existingUser.setLastName(userDTO.lastName());
        existingUser.setPhone(userDTO.phone());
        existingUser.setCountry(userDTO.country());
        existingUser.setCompanyType(userDTO.companyType());
        existingUser.setNewsletter(userDTO.newsletter());

        User updatedUser = userRepository.save(existingUser);
        return UserDTO.fromEntity(updatedUser);
    }

    // Méthode pour supprimer un utilisateur
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }
}