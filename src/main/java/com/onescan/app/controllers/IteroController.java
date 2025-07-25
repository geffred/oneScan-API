package com.onescan.app.controllers;

import com.onescan.app.Entity.Commande;
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
     * Lance la connexion à la plateforme Itero.
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
     * Vérifie l'état de connexion actuel à Itero.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        boolean connected = iteroService.isLoggedIn();
        return ResponseEntity.ok(connected ? "Connecté" : "Non connecté");
    }

    /**
     * Récupère les commandes disponibles sur Itero.
     */
    @GetMapping("/commandes")
    public ResponseEntity<?> getCommandes() {
        List<Commande> commandes = iteroService.fetchCommandes();

        if (commandes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Aucune commande récupérée.");
        }

        return ResponseEntity.ok(commandes);
    }

    /**
     * Déconnecte et ferme la session utilisateur actuelle.
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
