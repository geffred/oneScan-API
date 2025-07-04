package com.onescan.app.controllers;

import com.onescan.app.services.IteroSeleniumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itero")
public class IteroController {

    private final IteroSeleniumService iteroService;

    public IteroController(IteroSeleniumService iteroService) {
        this.iteroService = iteroService;
    }

    /**
     * Endpoint pour lancer la connexion à Itero.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login() {
        String result = iteroService.login();

        if (result.startsWith("Connexion réussie") || result.equals("Déjà connecté.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    /**
     * Vérifie si l'utilisateur est actuellement connecté à Itero.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        boolean connected = iteroService.isLoggedIn();
        return ResponseEntity.ok("Statut : " + (connected ? "Connecté" : "Non connecté"));
    }

    /**
     * Récupère la liste des patients depuis Itero.
     */
    @GetMapping("/patients")
    public ResponseEntity<?> getPatients() {
        List<String> patients = iteroService.fetchPatients();

        // Si la première ligne contient une erreur, on renvoie 401 Unauthorized
        if (!patients.isEmpty() && patients.get(0).startsWith("Erreur")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(patients.get(0));
        }

        return ResponseEntity.ok(patients);
    }

    /**
     * Ferme le navigateur et réinitialise la session.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String result = iteroService.logout();

        if (result.equals("Déconnexion réussie.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}