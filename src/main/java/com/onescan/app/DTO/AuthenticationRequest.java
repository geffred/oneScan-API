package com.onescan.app.DTO;

public record AuthenticationRequest(
                String email,
                String password) {
}