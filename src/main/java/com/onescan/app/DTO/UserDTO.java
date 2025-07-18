package com.onescan.app.DTO;

import com.onescan.app.Entity.Role;
import com.onescan.app.Entity.User;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String country,
        String companyType,
        boolean newsletter,
        Role role) {

    // Méthode statique pour convertir une entité User en UserDTO
    public static UserDTO fromEntity(User user) {
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getCountry(),
                user.getCompanyType(),
                user.isNewsletter(),
                user.getRole());
    }

    // Méthode pour convertir un UserDTO en entité User (sans mot de passe)
    public User toEntity() {
        return User.builder()
                .id(this.id)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .email(this.email)
                .phone(this.phone)
                .country(this.country)
                .companyType(this.companyType)
                .newsletter(this.newsletter)
                .role(this.role)
                .build();
    }
}