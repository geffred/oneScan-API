package com.onescan.app.controllers;

import com.onescan.app.services.DexisIsConnectSeleniumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dexis-isconnect")
public class DexisIsConnectController {

    private final DexisIsConnectSeleniumService dexisService;

    public DexisIsConnectController(DexisIsConnectSeleniumService dexisService) {
        this.dexisService = dexisService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login() {
        String result = dexisService.login();

        if (result.startsWith("Connexion réussie") || result.equals("Déjà connecté.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        boolean connected = dexisService.isLoggedIn();
        return ResponseEntity.ok("Statut : " + (connected ? "Connecté" : "Non connecté"));
    }

    @GetMapping("/patients")
    public ResponseEntity<?> getPatients() {
        List<String> patients = dexisService.fetchPatients();

        if (!patients.isEmpty() && patients.get(0).startsWith("Erreur")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(patients.get(0));
        }

        return ResponseEntity.ok(patients);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String result = dexisService.logout();

        if (result.equals("Déconnexion réussie.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}