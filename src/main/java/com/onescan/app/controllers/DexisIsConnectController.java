package com.onescan.app.controllers;

import com.onescan.app.Entity.Commande;
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

    /**
     * Connexion à Dexis via Selenium.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login() {
        String result = dexisService.login();

        if (result.startsWith("Connexion réussie") || result.equals("Déjà connecté.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    /**
     * Vérifie le statut de connexion à Dexis.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        boolean connected = dexisService.isLoggedIn();
        return ResponseEntity.ok(connected ? "Connecté" : "Non connecté");
    }

    /**
     * Récupération des commandes depuis Dexis.
     */
    @GetMapping("/commandes")
    public ResponseEntity<?> getCommandes() {
        List<Commande> commandes = dexisService.fetchCommandes();

        if (commandes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucune commande trouvée.");
        }

        return ResponseEntity.ok(commandes);
    }

    /**
     * Déconnexion de Dexis.
     */
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
