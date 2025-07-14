package com.onescan.app.controllers;

import com.onescan.app.Entity.Commande;
import com.onescan.app.services.MeditLinkSeleniumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meditlink")
public class MeditLinkController {

    private final MeditLinkSeleniumService meditLinkService;

    public MeditLinkController(MeditLinkSeleniumService meditLinkService) {
        this.meditLinkService = meditLinkService;
    }

    /**
     * Endpoint pour se connecter à MeditLink
     */
    @PostMapping("/login")
    public ResponseEntity<String> login() {
        String result = meditLinkService.login();

        if (result.startsWith("Connexion réussie") || result.equals("Déjà connecté.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    /**
     * Vérifie le statut de connexion à MeditLink
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        boolean connected = meditLinkService.isLoggedIn();
        return ResponseEntity.ok("Statut MeditLink : " + (connected ? "Connecté" : "Non connecté"));
    }

    /**
     * Récupère la liste des commandes/patients depuis MeditLink
     */
    @GetMapping("/commandes")
    public ResponseEntity<?> getCommandes() {
        List<Commande> commandes = meditLinkService.fetchCommandes();

        if (commandes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body("Aucune commande trouvée sur MeditLink ou problème de connexion");
        }

        return ResponseEntity.ok(commandes);
    }

    /**
     * Déconnexion de MeditLink
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        String result = meditLinkService.logout();

        if (result.equals("Déconnexion réussie.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}