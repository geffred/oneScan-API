package com.onescan.app.controllers;

import com.onescan.app.Entity.Commande;
import com.onescan.app.services.ThreeShapeSeleniumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/threeshape")
public class ThreeShapeController {

    private final ThreeShapeSeleniumService threeShapeService;

    public ThreeShapeController(ThreeShapeSeleniumService threeShapeService) {
        this.threeShapeService = threeShapeService;
    }

    /**
     * Endpoint pour se connecter à ThreeShape.
     */
    @PostMapping("/login")
    public ResponseEntity<String> login() {
        String result = threeShapeService.login();

        if (result.startsWith("Connexion réussie") || result.equals("Déjà connecté.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    /**
     * Vérifie si l'utilisateur est actuellement connecté à ThreeShape.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        boolean connected = threeShapeService.isLoggedIn();
        return ResponseEntity.ok("Statut : " + (connected ? "Connecté" : "Non connecté"));
    }

    /**
     * Récupère la liste des commandes depuis ThreeShape.
     */
    @GetMapping("/commandes")
    public ResponseEntity<?> getCommandes() {
        List<Commande> commandes = threeShapeService.fetchCommandes();

        if (commandes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Erreur : Impossible de se connecter à 3Shape ou aucune donnée trouvée.");
        }

        return ResponseEntity.ok(commandes);
    }

    /**
     * Ferme le navigateur et réinitialise la session.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String result = threeShapeService.logout();

        if (result.equals("Déconnexion réussie.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }
}