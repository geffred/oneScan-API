package com.onescan.app.DTO;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String phone,
        String country,
        String companyType,
        boolean newsletter) {
}