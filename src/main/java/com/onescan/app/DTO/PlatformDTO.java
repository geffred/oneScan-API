package com.onescan.app.DTO;

import com.onescan.app.Entity.Platform;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PlatformDTO(
        Long id,
        @NotBlank String name,
        @Email String email,
        @NotBlank String password,
        Long userId) {

    // Méthode statique pour convertir Entity -> DTO
    public static PlatformDTO fromEntity(Platform platform) {
        return new PlatformDTO(
                platform.getId(),
                platform.getName(),
                platform.getEmail(),
                null, // On ne expose pas le mot de passe dans le DTO
                platform.getUser() != null ? platform.getUser().getId() : null);
    }

    // Méthode pour convertir DTO -> Entity (pour la création)
    public Platform toEntity() {
        Platform platform = new Platform();
        platform.setName(this.name);
        platform.setEmail(this.email);
        platform.setPassword(this.password);
        return platform;
    }
}