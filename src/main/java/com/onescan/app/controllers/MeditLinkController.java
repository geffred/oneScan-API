package com.onescan.app.controllers;

import com.onescan.app.Entity.Commande;
import com.onescan.app.services.MeditLinkSeleniumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    /**
     * Récupère le commentaire d'une commande spécifique par son ID externe
     */
    @GetMapping("/commentaire/{externalId}")
    public ResponseEntity<?> getCommentaire(@PathVariable Long externalId) {
        try {
            String commentaire = meditLinkService.getCommentaire(externalId);

            if (commentaire == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Aucun commentaire trouvé pour la commande ID: " + externalId);
            }

            if (commentaire.isEmpty()) {
                return ResponseEntity.ok("Commentaire vide pour la commande ID: " + externalId);
            }

            return ResponseEntity.ok(Map.of(
                    "externalId", externalId,
                    "commentaire", commentaire));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération du commentaire: " + e.getMessage());
        }
    }

    /**
     * Récupère tous les commentaires pour toutes les commandes
     */
    @GetMapping("/commentaires")
    public ResponseEntity<?> getAllCommentaires() {
        try {
            Map<Long, String> commentaires = meditLinkService.getAllCommentaires();

            if (commentaires.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body("Aucun commentaire trouvé ou problème de connexion");
            }

            return ResponseEntity.ok(Map.of(
                    "total", commentaires.size(),
                    "commentaires", commentaires));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la récupération des commentaires: " + e.getMessage());
        }
    }

}